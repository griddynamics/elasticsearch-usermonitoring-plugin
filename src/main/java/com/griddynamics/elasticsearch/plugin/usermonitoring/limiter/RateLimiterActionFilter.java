package com.griddynamics.elasticsearch.plugin.usermonitoring.limiter;

import com.google.common.util.concurrent.RateLimiter;
import com.griddynamics.elasticsearch.plugin.usermonitoring.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.xpack.core.security.user.User;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class RateLimiterActionFilter implements ActionFilter {


    private final Logger logger = LogManager.getLogger(RateLimiterActionFilter.class);
    private final ConcurrentMap<String, UserLimiter> usersLimitersMap = new ConcurrentHashMap<>();
    private final SetOnce<ThreadContext> threadContext;
    private final RateLimiterConfiguration config;

    public RateLimiterActionFilter(SetOnce<ThreadContext> threadContext, RateLimiterConfiguration config) {
        this.threadContext = threadContext;
        this.config = config;
    }

    @Override
    public int order() {
        //Next after X-Pack SecurityActionFilter
        return Integer.MIN_VALUE + 1;
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void apply(Task task, String action,
                                                                                       Request request,
                                                                                       ActionListener<Response> listener,
                                                                                       ActionFilterChain<Request, Response> chain) {
        User user;
        if (action != null
            && action.startsWith("indices:data/read") //TODO property
            && !isSystemIndexRequest(request)
            && (user = Utils.extractUser(threadContext.get())) != null
            && !Utils.isSystemUser(user)) {

                final UserLimiter userLimiter = usersLimitersMap.computeIfAbsent(user.principal(), key -> new UserLimiter(config.getRateLimitValue()));
                final int countParallel = userLimiter.parallelRequests.incrementAndGet();
                try {
                    if (countParallel > config.getMaxParallel()) {
                        String msg = "User [" + user.principal() + "] reached maximum parallel requests and has been rejected";
                        logger.warn(msg);
                        listener.onFailure(new UserRejectedException(msg));
                        return;
                    }
                    if (countParallel >= config.getWarParallel()) {
                        logger.warn("User [" + user.principal() + "] do [" + countParallel + "] parallel requests");
                    }
                    if (config.getRateLimitEnable()) {
                        if (!userLimiter.rateLimiter.tryAcquire(1, config.getRateLimitWaitingTimeSec(), TimeUnit.SECONDS)) {
                            listener.onFailure(new UserRejectedException("User [" + user.principal() + "] has too many requests, " +
                                    "which can not go through the rate limiter at " + config.getRateLimitWaitingTimeSec() + " seconds. Request rejected."));
                            return;
                        }
                    }
                    chain.proceed(task, action, request, listener);
                } finally {
                    int result = userLimiter.parallelRequests.decrementAndGet();
                    if (result <= 0) {
                        //Remove user-information from map, if no more parallel requests
                        usersLimitersMap.compute(user.principal(), (key, current) -> {
                            if (current == null || (current == userLimiter && current.parallelRequests.get() <= 0)) {
                                return null;
                            }
                            return current;
                        });
                    }
                }
        } else {
            chain.proceed(task, action, request, listener);
        }

    }

    private <Request extends ActionRequest> boolean isSystemIndexRequest(Request request) {
        if (request instanceof SearchRequest && allStartWithDot(((SearchRequest)request).indices())) {
            return true;
        } else if (request instanceof GetRequest && allStartWithDot(((GetRequest)request).indices())) {
            return true;
        }
        return false;
    }

    private boolean allStartWithDot(String[] indices) {
        if (indices == null || indices.length == 0) {
            return false;
        } else {
            for (String index : indices) {
                if (index == null || index.length() <= 0 || index.charAt(0) != '.') {
                    return false;
                }
            }
            return true;
        }
    }

    private static class UserLimiter {
        private final RateLimiter rateLimiter;
        private final AtomicInteger parallelRequests = new AtomicInteger(0);

        UserLimiter(double permitsPerSecond) {
            rateLimiter = RateLimiter.create(permitsPerSecond);
        }

    }
}

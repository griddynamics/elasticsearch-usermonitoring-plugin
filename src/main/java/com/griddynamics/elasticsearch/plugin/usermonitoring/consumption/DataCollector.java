package com.griddynamics.elasticsearch.plugin.usermonitoring.consumption;

import com.griddynamics.elasticsearch.plugin.usermonitoring.Utils;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.index.shard.SearchOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.xpack.core.security.user.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataCollector implements SearchOperationListener {

    private final ThreadContext threadContext;
    private final Set<String> skipUsers;

    private final ConcurrentMap<String, UserData> userDataConcurrentMap = new ConcurrentHashMap<>();

    public DataCollector(ThreadContext threadContext, UserConsumptionConfiguration configuration) {
        this.threadContext = threadContext;
        this.skipUsers = Collections.unmodifiableSet(new HashSet<>(configuration.getSkipUsers()));
    }


    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {

        User user = Utils.extractUser(threadContext);
        String userName;
        if (user != null && (userName = user.principal()) != null && !skipUsers.contains(userName)) {
            final ShardId shardId = searchContext.indexShard().shardId();
            UserData userData = userDataConcurrentMap.computeIfAbsent(userName, key -> new UserData(shardId, tookInNanos));
            userData.add(shardId, tookInNanos);
            /*userDataConcurrentMap.compute(userName, (key, userData) -> { //Compute can be executed multiple time
                if (userData == null) {
                    return ;
                } else {
                    userData.add(shardId, tookInNanos);
                    return userData;
                }
            });*/


        }
    }

    public Map<String, UserData> getAndResetData() {
        synchronized (userDataConcurrentMap) { //By inner object
            Set<String> userNames = new HashSet<>(userDataConcurrentMap.keySet());
            Map<String, UserData> result = new HashMap<>(userNames.size());
            for (String username : userNames) {
                UserData userData = userDataConcurrentMap.remove(username);
                if (userData != null) {
                    result.put(username, userData);
                }
            }
            return result;
        }
    }

}

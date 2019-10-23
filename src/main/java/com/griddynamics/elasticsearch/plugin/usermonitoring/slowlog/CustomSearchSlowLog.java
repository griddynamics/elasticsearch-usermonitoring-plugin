/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.griddynamics.elasticsearch.plugin.usermonitoring.slowlog;

import com.griddynamics.elasticsearch.plugin.usermonitoring.Utils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.SlowLogLevel;
import org.elasticsearch.index.shard.SearchOperationListener;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.xpack.core.security.user.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.SearchSlowLog.*;

public final class CustomSearchSlowLog implements SearchOperationListener {
    private long queryWarnThreshold;
    private long queryInfoThreshold;
    private long queryDebugThreshold;
    private long queryTraceThreshold;

    private long fetchWarnThreshold;
    private long fetchInfoThreshold;
    private long fetchDebugThreshold;
    private long fetchTraceThreshold;

    private SlowLogLevel level;

    private final Logger queryLogger;
    private Logger queryLoggerOrigin;
    private final Logger fetchLogger;
    private Logger fetchLoggerOrigin;

    private static final String INDEX_SEARCH_SLOWLOG_PREFIX = "index.search.custom.slowlog";
    private static final String INDEX_SEARCH_SLOWLOG_ORIGIN_PREFIX = "index.search.slowlog";


    private static final ToXContent.Params FORMAT_PARAMS = new ToXContent.MapParams(Collections.singletonMap("pretty", "false"));
    private final ThreadContext threadContext;
    private final SlowLogConfiguration slowLogConfiguration;

    public CustomSearchSlowLog(IndexSettings indexSettings, ThreadContext threadContext,
                               boolean isOriginalRemovedSuccess, boolean appendRoles) {
        this.threadContext = threadContext;
        this.slowLogConfiguration = new SlowLogConfiguration(appendRoles);


        final String prefix = isOriginalRemovedSuccess ? INDEX_SEARCH_SLOWLOG_ORIGIN_PREFIX : INDEX_SEARCH_SLOWLOG_PREFIX;
        this.queryLogger = LogManager.getLogger(prefix + ".query");
        this.fetchLogger = LogManager.getLogger(prefix + ".fetch");
        if (!isOriginalRemovedSuccess) {
            this.queryLoggerOrigin = LogManager.getLogger(INDEX_SEARCH_SLOWLOG_ORIGIN_PREFIX + ".query");
            this.fetchLoggerOrigin = LogManager.getLogger(INDEX_SEARCH_SLOWLOG_ORIGIN_PREFIX + ".fetch");
        }


        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_WARN_SETTING,
            this::setQueryWarnThreshold);
        this.queryWarnThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_WARN_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_INFO_SETTING,
            this::setQueryInfoThreshold);
        this.queryInfoThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_INFO_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_DEBUG_SETTING,
            this::setQueryDebugThreshold);
        this.queryDebugThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_DEBUG_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_TRACE_SETTING,
            this::setQueryTraceThreshold);
        this.queryTraceThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_TRACE_SETTING).nanos();

        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_WARN_SETTING,
            this::setFetchWarnThreshold);
        this.fetchWarnThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_WARN_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_INFO_SETTING,
            this::setFetchInfoThreshold);
        this.fetchInfoThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_INFO_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_DEBUG_SETTING,
            this::setFetchDebugThreshold);
        this.fetchDebugThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_DEBUG_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_TRACE_SETTING,
            this::setFetchTraceThreshold);
        this.fetchTraceThreshold = indexSettings.getValue(INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_TRACE_SETTING).nanos();

        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_SEARCH_SLOWLOG_LEVEL, this::setLevel);
        setLevel(indexSettings.getValue(INDEX_SEARCH_SLOWLOG_LEVEL));
    }

    private void setLevel(SlowLogLevel level) {
        this.level = level;
        Loggers.setLevel(queryLogger, level.name());
        Loggers.setLevel(fetchLogger, level.name());
        if (queryLoggerOrigin != null && fetchLoggerOrigin != null) {
            Loggers.setLevel(queryLoggerOrigin, Level.OFF);
            Loggers.setLevel(fetchLoggerOrigin, Level.OFF);
        }
    }

    @Override
    public void onQueryPhase(SearchContext context, long tookInNanos) {
        if (queryWarnThreshold >= 0 && tookInNanos > queryWarnThreshold) {
            queryLogger.warn("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        } else if (queryInfoThreshold >= 0 && tookInNanos > queryInfoThreshold) {
            queryLogger.info("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        } else if (queryDebugThreshold >= 0 && tookInNanos > queryDebugThreshold) {
            queryLogger.debug("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        } else if (queryTraceThreshold >= 0 && tookInNanos > queryTraceThreshold) {
            queryLogger.trace("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        }
    }

    @Override
    public void onFetchPhase(SearchContext context, long tookInNanos) {
        if (fetchWarnThreshold >= 0 && tookInNanos > fetchWarnThreshold) {
            fetchLogger.warn("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        } else if (fetchInfoThreshold >= 0 && tookInNanos > fetchInfoThreshold) {
            fetchLogger.info("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        } else if (fetchDebugThreshold >= 0 && tookInNanos > fetchDebugThreshold) {
            fetchLogger.debug("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        } else if (fetchTraceThreshold >= 0 && tookInNanos > fetchTraceThreshold) {
            fetchLogger.trace("{}", new SlowLogSearchContextPrinter(context, tookInNanos, threadContext, slowLogConfiguration));
        }
    }

    static final class SlowLogSearchContextPrinter {
        private final SearchContext context;
        private final long tookInNanos;
        private final ThreadContext threadContext;
        private SlowLogConfiguration configuration;

        SlowLogSearchContextPrinter(SearchContext context, long tookInNanos, ThreadContext threadContext,
                                    SlowLogConfiguration configuration) {
            this.context = context;
            this.tookInNanos = tookInNanos;
            this.threadContext = threadContext;
            this.configuration = configuration;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(context.indexShard().shardId())
                .append(" ")
                .append("took[").append(TimeValue.timeValueNanos(tookInNanos)).append("], ")
                .append("took_millis[").append(TimeUnit.NANOSECONDS.toMillis(tookInNanos)).append("], ")
                .append("total_hits[").append(context.queryResult().getTotalHits()).append("], ");
            if (context.getQueryShardContext().getTypes() == null) {
                sb.append("types[], ");
            } else {
                sb.append("types[");
                Strings.arrayToDelimitedString(context.getQueryShardContext().getTypes(), ",", sb);
                sb.append("], ");
            }
            if (context.groupStats() == null) {
                sb.append("stats[], ");
            } else {
                sb.append("stats[");
                Strings.collectionToDelimitedString(context.groupStats(), ",", "", "", sb);
                sb.append("], ");
            }
            sb.append("search_type[").append(context.searchType()).append("], total_shards[")
                .append(context.numberOfShards()).append("], ");

            User user = Utils.extractUser(threadContext);
            if (user != null) {
                sb.append("username[").append(user.principal()).append("], ");
                if (configuration.isAppendRoles()) {
                    sb.append("roles").append(Arrays.toString(user.roles())).append("], ");

                }
            }

            if (context.request().source() != null) {
                sb.append("source[").append(context.request().source().toString(FORMAT_PARAMS)).append("], ");
            } else {
                sb.append("source[], ");
            }



            return sb.toString();
        }
    }


    private void setQueryWarnThreshold(TimeValue warnThreshold) {
        this.queryWarnThreshold = warnThreshold.nanos();
    }

    private void setQueryInfoThreshold(TimeValue infoThreshold) {
        this.queryInfoThreshold = infoThreshold.nanos();
    }

    private void setQueryDebugThreshold(TimeValue debugThreshold) {
        this.queryDebugThreshold = debugThreshold.nanos();
    }

    private void setQueryTraceThreshold(TimeValue traceThreshold) {
        this.queryTraceThreshold = traceThreshold.nanos();
    }

    private void setFetchWarnThreshold(TimeValue warnThreshold) {
        this.fetchWarnThreshold = warnThreshold.nanos();
    }

    private void setFetchInfoThreshold(TimeValue infoThreshold) {
        this.fetchInfoThreshold = infoThreshold.nanos();
    }

    private void setFetchDebugThreshold(TimeValue debugThreshold) {
        this.fetchDebugThreshold = debugThreshold.nanos();
    }

    private void setFetchTraceThreshold(TimeValue traceThreshold) {
        this.fetchTraceThreshold = traceThreshold.nanos();
    }

    long getQueryWarnThreshold() {
        return queryWarnThreshold;
    }

    long getQueryInfoThreshold() {
        return queryInfoThreshold;
    }

    long getQueryDebugThreshold() {
        return queryDebugThreshold;
    }

    long getQueryTraceThreshold() {
        return queryTraceThreshold;
    }

    long getFetchWarnThreshold() {
        return fetchWarnThreshold;
    }

    long getFetchInfoThreshold() {
        return fetchInfoThreshold;
    }

    long getFetchDebugThreshold() {
        return fetchDebugThreshold;
    }

    long getFetchTraceThreshold() {
        return fetchTraceThreshold;
    }

    SlowLogLevel getLevel() {
        return level;
    }

}

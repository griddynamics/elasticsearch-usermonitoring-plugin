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
package com.griddynamics.elasticsearch.plugin.usermonitoring.limiter;


import com.griddynamics.elasticsearch.plugin.usermonitoring.AbstractFeature;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.core.XPackPlugin;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class RequestLimiterFeature extends AbstractFeature {


    private final boolean transportClientMode;
    private final SetOnce<ThreadContext> threadContext = new SetOnce<>();
    private final RateLimiterConfiguration rateLimiterConfiguration;


    public RequestLimiterFeature(String mainPrefix, Settings settings) {
        super(mainPrefix + "request.limiter", settings);
        this.transportClientMode = XPackPlugin.transportClientMode(settings);
        this.rateLimiterConfiguration = new RateLimiterConfiguration(getSettingsPrefix(), settings);
    }

    public void createComponents(ThreadPool threadPool) {
        threadContext.set(threadPool.getThreadContext());
    }

    public List<ActionFilter> getActionFilters() {
        if (!isEnabled()) {
            return emptyList();
        }
        // registering the security filter only for nodes
        if (!transportClientMode) {
            return singletonList(new RateLimiterActionFilter(threadContext, rateLimiterConfiguration));
        }
        return emptyList();
    }


    @Override
    protected List<Setting<?>> getConfigurationSettings() {
        return rateLimiterConfiguration.getAllSettings();
    }


}



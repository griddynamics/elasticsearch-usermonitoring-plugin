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
package com.griddynamics.elasticsearch.plugins.usermonitoring;


import com.griddynamics.elasticsearch.plugins.usermonitoring.consumers.UserDataLoggerService;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


public class CustomUsermonitoringPlugin extends Plugin {


    public final static String PLUGIN_SETTINGS_PREFIX = "plugin.custom.usermonitoring.";
    final static String THREAD_POOL_NAME = "usermonitoring";

    private static final Setting<Boolean> USERMONITORING_PLUGIN_ENABLED = Setting.boolSetting(PLUGIN_SETTINGS_PREFIX + "enabled", true, Setting.Property.NodeScope);
    private static final Setting<Integer> CONSUMING_INTERVAL_SECONDS = Setting.intSetting(PLUGIN_SETTINGS_PREFIX + "interval.seconds", 60, 1, Setting.Property.NodeScope);
    private static final Setting<List<String>> SKIP_INDICIES = Setting.listSetting(PLUGIN_SETTINGS_PREFIX + "skip.indicies.prefix", Collections.emptyList(), Function.identity(), Setting.Property.NodeScope);
    static List<Setting<?>> ALL_SETTINGS = Arrays.asList(
            USERMONITORING_PLUGIN_ENABLED,
            SKIP_INDICIES,
            CONSUMING_INTERVAL_SECONDS
    );

    private final SetOnce<DataCollector> dataCollector = new SetOnce<>();
    private final Settings settings;
    private final List<Consumer<Map<String, UserData>>> userDataConsumers = new ArrayList<>(2);
    private final boolean enabled;
    private final List<String> indexPrefixes = new ArrayList<>(); //TODO tree

    public CustomUsermonitoringPlugin(Settings settings) {
        this.settings = settings;
        this.enabled = USERMONITORING_PLUGIN_ENABLED.get(settings);
        this.indexPrefixes.addAll(SKIP_INDICIES.get(settings));
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService,
                                               ScriptService scriptService, NamedXContentRegistry xContentRegistry,
                                               Environment environment, NodeEnvironment nodeEnvironment,
                                               NamedWriteableRegistry namedWriteableRegistry) {
        if (enabled) {
            dataCollector.set(new DataCollector(threadPool.getThreadContext(), settings));
            userDataConsumers.add(new UserDataLoggerService(settings));
            threadPool.scheduleWithFixedDelay(this::consume, TimeValue.timeValueSeconds(
                    CONSUMING_INTERVAL_SECONDS.get(settings)
            ), THREAD_POOL_NAME); //TODO setting
        }
        return super.createComponents(client, clusterService, threadPool, resourceWatcherService, scriptService, xContentRegistry, environment, nodeEnvironment, namedWriteableRegistry);
    }



    private void consume() {
        Map<String, UserData> userDataMap = dataCollector.get().getAndResetData();
        if (userDataMap != null && !userDataMap.isEmpty()) {
            for (Consumer<Map<String, UserData>> userDataConsumer : userDataConsumers) {
                userDataConsumer.accept(userDataMap);
            }
        }
    }

    @Override
    public void onIndexModule(IndexModule indexModule) {
        String name = indexModule.getIndex().getName();
        if (enabled && !hasPrefix(name)) {
            indexModule.addSearchOperationListener(dataCollector.get());
        }
    }

    private boolean hasPrefix(String name) {
        for (String indexPrefix : indexPrefixes) {
            if (name.startsWith(indexPrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> allSettings = new ArrayList<>();
        allSettings.addAll(ALL_SETTINGS);
        allSettings.addAll(UserDataLoggerService.ALL_SETTINGS);
        allSettings.addAll(DataCollector.ALL_SETTINGS);
        return allSettings;
    }

    @Override
    public List<ExecutorBuilder<?>> getExecutorBuilders(final Settings settings) {
        if (enabled) {
            final FixedExecutorBuilder builder =
                    new FixedExecutorBuilder(
                            settings,
                            THREAD_POOL_NAME,
                            1,
                            1,
                            PLUGIN_SETTINGS_PREFIX + "thread_pool");
            return Collections.singletonList(builder);
        }
        return Collections.emptyList();
    }


}

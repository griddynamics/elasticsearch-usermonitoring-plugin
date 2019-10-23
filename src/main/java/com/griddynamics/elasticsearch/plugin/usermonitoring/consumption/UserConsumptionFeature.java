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
package com.griddynamics.elasticsearch.plugin.usermonitoring.consumption;


import com.griddynamics.elasticsearch.plugin.usermonitoring.AbstractFeature;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class UserConsumptionFeature extends AbstractFeature {


    final static String THREAD_POOL_NAME = "userconsumption";

    private final SetOnce<DataCollector> dataCollector = new SetOnce<>();
    private final List<Consumer<Map<String, UserData>>> userDataConsumers = new ArrayList<>(2);
    private final List<String> indexPrefixes = new ArrayList<>(); //TODO tree
    private UserConsumptionConfiguration configuration;



    public UserConsumptionFeature(String mainPrefix, Settings settings) {
        super(mainPrefix + "consumers", settings);
        this.configuration = new UserConsumptionConfiguration(getSettingsPrefix(), settings);

    }

    public void createComponents(ThreadPool threadPool) {

        dataCollector.set(new DataCollector(threadPool.getThreadContext(), configuration));
        userDataConsumers.add(new UserDataLoggerService(configuration));
        threadPool.scheduleWithFixedDelay(this::consume, TimeValue.timeValueSeconds(
                configuration.getConsumingIntervalSeconds()
        ), THREAD_POOL_NAME);
    }




    private void consume() {
        Map<String, UserData> userDataMap = dataCollector.get().getAndResetData();
        if (userDataMap != null && !userDataMap.isEmpty()) {
            for (Consumer<Map<String, UserData>> userDataConsumer : userDataConsumers) {
                userDataConsumer.accept(userDataMap);
            }
        }
    }

    public void acceptIndexModule(IndexModule indexModule) {
        String name = indexModule.getIndex().getName();
        if (!hasPrefix(name)) {
            indexModule.addSearchOperationListener(dataCollector.get());
        }
    }

    private boolean hasPrefix(String name) {
        for (String indexPrefix : configuration.getSkipIndicies()) {
            if (name.startsWith(indexPrefix)) {
                return true;
            }
        }
        return false;
    }



    @Override
    protected List<Setting<?>> getConfigurationSettings() {
        return configuration.getSettings();
    }


    public List<ExecutorBuilder<?>> executorBuilder(Settings settings) {
        if (isEnabled()) {
            final FixedExecutorBuilder builder =
                    new FixedExecutorBuilder(
                            settings,
                            THREAD_POOL_NAME,
                            1,
                            1,
                            getSettingsPrefix() + "thread_pool");
            return Collections.singletonList(builder);
        } else {
            return Collections.emptyList();
        }
    }


}

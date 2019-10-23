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


import com.griddynamics.elasticsearch.plugin.usermonitoring.AbstractFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.SearchSlowLog;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.griddynamics.elasticsearch.plugin.usermonitoring.Utils.extract;


public class SlowLogFeature extends AbstractFeature {
    private final Setting<Boolean> appendRolesSetting;
    private final boolean appendRolesValue;

    private Logger logger = LogManager.getLogger(SlowLogFeature.class);
    private final SetOnce<ThreadContext> threadContext = new SetOnce<>();

    public SlowLogFeature(String mainPrefix, Settings settings) {
        super(mainPrefix + "slowlog", settings);
        appendRolesSetting = Setting.boolSetting(getSettingsPrefix() + "append.roles", false, Setting.Property.NodeScope);
        appendRolesValue = appendRolesSetting.get(settings);
    }

    @Override
    protected List<Setting<?>> getConfigurationSettings() {
        return Collections.singletonList(appendRolesSetting);
    }

    public void createComponents(ThreadContext threadContext) {
        this.threadContext.set(threadContext);
    }

    public void acceptIndexModule(IndexModule indexModule) {
        if (isEnabled()) {
            IndexSettings indexSettings = extract("indexSettings", IndexSettings.class, indexModule);
            boolean success = removeStandardSlowlog(indexModule);
            CustomSearchSlowLog listener = new CustomSearchSlowLog(indexSettings, threadContext.get(), success, appendRolesValue);
            indexModule.addSearchOperationListener(listener);
        }
    }

    private boolean removeStandardSlowlog(IndexModule indexModule) {
        List searchOperationListeners = extract("searchOperationListeners", List.class, indexModule);
        if (searchOperationListeners != null) {
            for (Iterator iterator = searchOperationListeners.iterator(); iterator.hasNext(); ) {
                Object searchOperationListener = iterator.next();
                if (searchOperationListener instanceof SearchSlowLog) {
                    iterator.remove();
                    logger.debug("Original SearchSlowLog removed success");
                    return true;
                }

            }
        }
        logger.warn("Can not remove standard slowlog for index:" + indexModule.getIndex().getName());
        return false;

    }

}

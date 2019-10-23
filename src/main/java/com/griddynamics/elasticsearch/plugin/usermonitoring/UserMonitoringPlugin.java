package com.griddynamics.elasticsearch.plugin.usermonitoring;

import com.griddynamics.elasticsearch.plugin.usermonitoring.consumption.UserConsumptionFeature;
import com.griddynamics.elasticsearch.plugin.usermonitoring.limiter.RequestLimiterFeature;
import com.griddynamics.elasticsearch.plugin.usermonitoring.slowlog.SlowLogFeature;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class UserMonitoringPlugin extends Plugin implements ActionPlugin {

    public final static String MAIN_PLUGIN_SETTINGS_PREFIX = "plugin.custom.usermonitoring.";


    private static final Setting<Boolean> CUSTOM_PLUGIN_ENABLED = Setting.boolSetting(MAIN_PLUGIN_SETTINGS_PREFIX + "enabled", true, Setting.Property.NodeScope);


    private final SlowLogFeature slowLogFeature;
    private final RequestLimiterFeature requestLimiterFeature;
    private final UserConsumptionFeature userConsumptionFeature;


    private List<Consumer<IndexModule>> onIndexChain = new ArrayList<>(3);
    private final Boolean enabled;


    public UserMonitoringPlugin(Settings settings) {
        enabled = CUSTOM_PLUGIN_ENABLED.get(settings);
        this.slowLogFeature = new SlowLogFeature(MAIN_PLUGIN_SETTINGS_PREFIX, settings);
        this.requestLimiterFeature = new RequestLimiterFeature(MAIN_PLUGIN_SETTINGS_PREFIX, settings);
        this.userConsumptionFeature = new UserConsumptionFeature(MAIN_PLUGIN_SETTINGS_PREFIX, settings);


        //this.indexPrefixes.addAll(SKIP_INDICIES.get(settings));
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService,
                                               ThreadPool threadPool, ResourceWatcherService resourceWatcherService,
                                               ScriptService scriptService, NamedXContentRegistry xContentRegistry,
                                               Environment environment, NodeEnvironment nodeEnvironment,
                                               NamedWriteableRegistry namedWriteableRegistry) {
        slowLogFeature.createComponents(threadPool.getThreadContext());
        if (slowLogFeature.isEnabled()) {
            onIndexChain.add(slowLogFeature::acceptIndexModule);
        }
        requestLimiterFeature.createComponents(threadPool);

        userConsumptionFeature.createComponents(threadPool);
        if (userConsumptionFeature.isEnabled()) {
            onIndexChain.add(userConsumptionFeature::acceptIndexModule);
        }
        return Collections.emptyList();
    }

    @Override
    public void onIndexModule(IndexModule indexModule) {
        if (enabled) {
            onIndexChain.forEach(indexModuleConsumer -> indexModuleConsumer.accept(indexModule));
        }
    }

    @Override
    public List<ExecutorBuilder<?>> getExecutorBuilders(final Settings settings) {
        return userConsumptionFeature.executorBuilder(settings);
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Stream.of(
                slowLogFeature.getSettings(),
                requestLimiterFeature.getSettings(),
                userConsumptionFeature.getSettings(),
                Collections.singletonList(CUSTOM_PLUGIN_ENABLED)
        ).flatMap(List::stream).collect(toList());
    }

    @Override
    public List<ActionFilter> getActionFilters() {
        if (enabled) {
            return requestLimiterFeature.getActionFilters();
        } else {
            return Collections.emptyList();
        }
    }

}

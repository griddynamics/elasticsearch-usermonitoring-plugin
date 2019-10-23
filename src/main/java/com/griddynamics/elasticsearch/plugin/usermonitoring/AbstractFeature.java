package com.griddynamics.elasticsearch.plugin.usermonitoring;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFeature {

    private final String prefix;
    protected final Settings settings;
    protected final Setting<Boolean> enabledSetting;
    private final boolean enabled;

    public AbstractFeature(String prefix, Settings settings) {
        this.prefix = prefix + ".";
        this.settings = settings;
        this.enabledSetting = Setting.boolSetting(getSettingsPrefix() + "enabled", defaultEnabled(), Setting.Property.NodeScope);
        this.enabled = enabledSetting.get(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected String getSettingsPrefix() {
        return prefix;
    }

    protected boolean defaultEnabled() {
        return true;
    }

    public final List<Setting<?>> getSettings() {
        List<Setting<?>> configurationSettings = getConfigurationSettings();
        int initSize = 1;
        if (configurationSettings != null) {
            initSize += configurationSettings.size();
        }
        List<Setting<?>> result = new ArrayList<>(initSize);
        if (configurationSettings != null) {
            result.addAll(configurationSettings);
        }
        result.add(enabledSetting);
        return result;
    }

    protected abstract List<Setting<?>> getConfigurationSettings();
}

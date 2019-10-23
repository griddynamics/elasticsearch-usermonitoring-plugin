package com.griddynamics.elasticsearch.plugin.usermonitoring.limiter;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;

import java.util.Arrays;
import java.util.List;

public class RateLimiterConfiguration {
    private final Setting<Boolean> rateLimitEnabledSetting;
    private final Setting<Double> rateLimitValueSetting;
    private final Setting<Integer> rateLimitWaitingTimeSetting;
    private final Setting<Integer> maximumParallelSetting;
    private final Setting<Integer> warnParallelSetting;

    private final Boolean rateLimitEnable;
    private final Double rateLimitValue;
    private final Integer rateLimitWaitingTimeSec;
    private final Integer maxParallel;
    private final Integer warParallel;

    public RateLimiterConfiguration(String prefix, Settings settings) {
        rateLimitEnabledSetting = Setting.boolSetting(prefix + "ratelimit.enabled", true, Setting.Property.NodeScope);
        rateLimitEnable = rateLimitEnabledSetting.get(settings);
        rateLimitValueSetting = Setting.doubleSetting(prefix + "ratelimit.permitsPerSecond", 2d, 0d, Setting.Property.NodeScope);
        rateLimitValue = rateLimitValueSetting.get(settings);
        rateLimitWaitingTimeSetting = Setting.intSetting(prefix + "ratelimit.waitingTimeSec", 30, 0, Setting.Property.NodeScope);
        rateLimitWaitingTimeSec = rateLimitWaitingTimeSetting.get(settings);
        maximumParallelSetting = Setting.intSetting(prefix + "parallel.max", 10, 1, Setting.Property.NodeScope);
        maxParallel = maximumParallelSetting.get(settings);
        warnParallelSetting = Setting.intSetting(prefix + "parallel.warn", 7, 0, Setting.Property.NodeScope);
        warParallel = warnParallelSetting.get(settings);


    }

    public Boolean getRateLimitEnable() {
        return rateLimitEnable;
    }

    public Double getRateLimitValue() {
        return rateLimitValue;
    }

    public Integer getRateLimitWaitingTimeSec() {
        return rateLimitWaitingTimeSec;
    }

    public Integer getMaxParallel() {
        return maxParallel;
    }

    public Integer getWarParallel() {
        return warParallel;
    }

    public List<Setting<?>> getAllSettings() {
        return Arrays.asList(rateLimitEnabledSetting, rateLimitValueSetting, rateLimitWaitingTimeSetting,
                maximumParallelSetting, warnParallelSetting);
    }
}

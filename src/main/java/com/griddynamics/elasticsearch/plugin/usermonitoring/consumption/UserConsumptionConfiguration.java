package com.griddynamics.elasticsearch.plugin.usermonitoring.consumption;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class UserConsumptionConfiguration {
    private final Setting<Integer> consumingIntervalSecondsSetting;
    private final Integer consumingIntervalSeconds;

    private final Setting<List<String>> skipIndiciesSetting;
    private final List<String> skipIndicies;

    private final Setting<List<String>> skipUsersSetting;
    private final List<String> skipUsers;

    private final Setting<UserDataLoggerService.LoggingDetails> loggingDetailsSetting;
    private final UserDataLoggerService.LoggingDetails loggingDetails;

    private final Setting<Boolean> skipZeroTimeSetting;
    private final Boolean skipZeroTime;

    public UserConsumptionConfiguration(String settingsPrefix, Settings settings) {
        consumingIntervalSecondsSetting = Setting.intSetting(settingsPrefix + "interval.seconds", 60, 1, Setting.Property.NodeScope);
        consumingIntervalSeconds = consumingIntervalSecondsSetting.get(settings);

        skipIndiciesSetting = Setting.listSetting(settingsPrefix + "skip.indicies.prefix", Collections.singletonList("."), Function.identity(), Setting.Property.NodeScope);
        skipIndicies = skipIndiciesSetting.get(settings);

        skipUsersSetting = Setting.listSetting(settingsPrefix + "skip.users", Arrays.asList("x_pack", "_xpack_security"), Function.identity(), Setting.Property.NodeScope);
        skipUsers = skipUsersSetting.get(settings);

        loggingDetailsSetting = new Setting<>(settingsPrefix + "logger.details", UserDataLoggerService.LoggingDetails.BY_INDEX.name(), UserDataLoggerService.LoggingDetails::parse, Setting.Property.NodeScope);
        loggingDetails = loggingDetailsSetting.get(settings);

        skipZeroTimeSetting = Setting.boolSetting(settingsPrefix + "skip.zerotime", true, Setting.Property.NodeScope);
        skipZeroTime = skipZeroTimeSetting.get(settings);
    }


    public List<Setting<?>> getSettings() {
        return Arrays.asList(consumingIntervalSecondsSetting, skipIndiciesSetting,
                skipUsersSetting, loggingDetailsSetting, skipZeroTimeSetting);
    }

    public Integer getConsumingIntervalSeconds() {
        return consumingIntervalSeconds;
    }

    public List<String> getSkipIndicies() {
        return skipIndicies;
    }

    public List<String> getSkipUsers() {
        return skipUsers;
    }

    public UserDataLoggerService.LoggingDetails getLoggingDetails() {
        return loggingDetails;
    }

    public Boolean getSkipZeroTime() {
        return skipZeroTime;
    }
}

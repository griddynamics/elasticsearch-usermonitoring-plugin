package com.griddynamics.elasticsearch.plugins.usermonitoring.consumers;


import com.griddynamics.elasticsearch.plugins.usermonitoring.UserData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.shard.ShardId;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.griddynamics.elasticsearch.plugins.usermonitoring.CustomUsermonitoringPlugin.PLUGIN_SETTINGS_PREFIX;
import static java.lang.Math.round;

public class UserDataLoggerService implements Consumer<Map<String, UserData>> {

    private static final Setting<LoggingDetails> LOGGING_DETAILS_SETTING_SETTING =
            new Setting<>(PLUGIN_SETTINGS_PREFIX + "logger.details", LoggingDetails.BY_INDEX.name(), LoggingDetails::parse, Setting.Property.NodeScope);

    private static final Setting<Boolean> SKIP_ZERO_TIME_SETTING = Setting.boolSetting(PLUGIN_SETTINGS_PREFIX + "skip.zerotime", true, Setting.Property.NodeScope);

    public static List<Setting<?>> ALL_SETTINGS = Arrays.asList(
            LOGGING_DETAILS_SETTING_SETTING,
            SKIP_ZERO_TIME_SETTING
    );

    private final Logger logger = LogManager.getLogger("custom.usermonitoring.logger");
    private final LoggingDetails loggingDetails;
    private final boolean skipZeroTime;

    public UserDataLoggerService(Settings settings) {
        this.loggingDetails = LOGGING_DETAILS_SETTING_SETTING.get(settings);
        this.skipZeroTime = SKIP_ZERO_TIME_SETTING.get(settings);
    }

    @Override
    public void accept(Map<String, UserData> stringUserDataMap) {
        for (Map.Entry<String, UserData> dataEntry : stringUserDataMap.entrySet()) {
            long total = 0;
            for (Map.Entry<ShardId, Long> shardIdLongEntry : dataEntry.getValue().getIndexShardTime().entrySet()) {
                if (loggingDetails == LoggingDetails.BY_INDEX) {
                    int val = round(shardIdLongEntry.getValue() / 1_000_000);
                    if (val != 0 || !skipZeroTime) {
                        logger.info("User[" + dataEntry.getKey() + "] consumed [" + val + "]ms for index[" + shardIdLongEntry.getKey().getIndexName() + "]");
                    }

                }
                total += shardIdLongEntry.getValue();
            }
            if (loggingDetails == LoggingDetails.TOTAL) {
                int val = round(total / 1_000_000);
                if (val != 0 || !skipZeroTime) {
                    logger.info("User[" + dataEntry.getKey() + "] consumed[" + val + "]ms");
                }
            }
        }
    }

    enum  LoggingDetails {
        TOTAL,
        BY_INDEX;

        public static LoggingDetails parse(String val) {
            return valueOf(val.toUpperCase(Locale.ROOT));
        }
    }
}

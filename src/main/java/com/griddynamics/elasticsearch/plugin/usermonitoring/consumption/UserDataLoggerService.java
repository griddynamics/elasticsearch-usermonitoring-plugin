package com.griddynamics.elasticsearch.plugin.usermonitoring.consumption;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.shard.ShardId;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Math.round;

public class UserDataLoggerService implements Consumer<Map<String, UserData>> {



    private final Logger logger = LogManager.getLogger("custom.usermonitoring.logger");
    private final UserConsumptionConfiguration configuration;

    public UserDataLoggerService(UserConsumptionConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void accept(Map<String, UserData> stringUserDataMap) {
        for (Map.Entry<String, UserData> dataEntry : stringUserDataMap.entrySet()) {
            long total = 0;
            for (Map.Entry<ShardId, Long> shardIdLongEntry : dataEntry.getValue().getIndexShardTime().entrySet()) {
                if (configuration.getLoggingDetails() == LoggingDetails.BY_INDEX) {
                    int val = round(shardIdLongEntry.getValue() / 1_000_000);
                    if (val != 0 || !configuration.getSkipZeroTime()) {
                        logger.info("User[" + dataEntry.getKey() + "] consumed [" + val + "]ms for index[" + shardIdLongEntry.getKey().getIndexName() + "]");
                    }

                }
                total += shardIdLongEntry.getValue();
            }
            if (configuration.getLoggingDetails() == LoggingDetails.TOTAL) {
                int val = round(total / 1_000_000);
                if (val != 0 || !configuration.getSkipZeroTime()) {
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

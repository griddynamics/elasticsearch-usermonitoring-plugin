Custom-usermonitoring plugin for elasticsearch
================================

##How to install plugin
First build the plugin. May be built both with Maven and Gradle. Maven requires java 8, but Gradle 
requires java 11.
###Instructions for Maven
1. From plugin root: 
`mvn clean install`
2. Ensure that `custom-usermonitoring-0.0.1-SNAPSHOT.zip` was generated in `{plugin_root}/target/releases`
3. Change directory to `{elasticsearch_root}/bin`
4. Remove explain plugin if it was previously installed:
`./elasticsearch-plugin remove custom-usermonitoring`
5. Install plugin:
`./elasticsearch-plugin install -b file:{plugin_root}/target/releases/custom-usermonitoring-0.0.1-SNAPSHOT.zip`
6. Run elasticsearch:
`./elasticsearch`

###Instructions for Gradle
1. From plugin root: 
`gradle clean build -x integTestRunner -x test`
2. Ensure that `custom-usermonitoring-0.0.1-SNAPSHOT.zip` was generated in `{plugin_root}/build/distributions`
3. Change directory to `{elasticsearch_root}/bin`
4. Remove explain plugin if it was previously installed:
`./elasticsearch-plugin remove custom-usermonitoring`
5. Install plugin:
`./elasticsearch-plugin install -b file:{plugin_root}/build/distributions/custom-usermonitoring-0.0.1-SNAPSHOT.zip`
6. Run elasticsearch:
`./elasticsearch`



#####Description
This plugin print information about how many CPU time consumed user for query by some period of time.
This plugin has to be installed on DATA nodes.

####Properties (default values)
To elasticsearch.yml
```
plugin.custom.usermonitoring:
    enabled: true
    slowlog:
      enabled: true
      append.roles: false
    request.limiter:
      enabled: true
      ratelimit:
        enabled: true  ##Enable\disable requests rate limit per user
        permitsPerSecond: 2  ##Count of requests which user can do per second
        waintingTimeSec: 30 ##In case of lock waiting time will be more this value, request fast fail 
      parallel:
        max: 10  ##If the user will have this value of concurrent requests at the same time, then the next request will be rejected 
        warn: 7  ##If the user will have more than this value of concurrent requests at the same time, then information about this activity will be added to log

    consumption:
      enabled: true
      interval.seconds: 60
      skip:
        zerotime: true
          users:
            - "x_pack"
            - "_xpack_security"
          indicies.prefix:
            - "."
```

Logging properties:

To log4j2.properties
```
appender.user_monitoring_rolling.type = RollingFile
appender.user_monitoring_rolling.name = user_monitoring_rolling
appender.user_monitoring_rolling.fileName = user_monitoring.log
appender.user_monitoring_rolling.layout.type = PatternLayout
appender.user_monitoring_rolling.layout.pattern = [%d{ISO8601}][%-5p][%-25c] [%node_name]%marker %.10000m%n
appender.user_monitoring_rolling.filePattern = user_monitoring-%d{yyyy-MM-dd}.log
appender.user_monitoring_rolling.policies.type = Policies
appender.user_monitoring_rolling.policies.time.type = TimeBasedTriggeringPolicy
appender.user_monitoring_rolling.policies.time.interval = 1

appender.user_monitoring_rolling.policies.time.modulate = true
logger.user_monitoring_rolling.name = custom.usermonitoring.logger
logger.user_monitoring_rolling.level = info
logger.user_monitoring_rolling.appenderRef.user_monitoring_rolling.ref = user_monitoring_rolling
logger.user_monitoring_rolling.additivity = false
```

######Example output:
```
[2019-10-08T17:18:05,237][INFO ][custom.usermonitoring.logger] [esd2] User[elastic] consumed [376]ms
[2019-10-08T17:18:15,288][INFO ][custom.usermonitoring.logger] [esd2] User[elastic] consumed [115]ms
```
package com.griddynamics.elasticsearch.plugin.usermonitoring.consumption;

public class DataCollectionException extends RuntimeException {
    public DataCollectionException(String msg) {
        super(msg);
    }

    public DataCollectionException(String msg, Throwable e) {
        super(msg, e);
    }
}

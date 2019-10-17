package com.griddynamics.elasticsearch.plugins.usermonitoring;

public class DataCollectionException extends RuntimeException {
    public DataCollectionException(String msg) {
        super(msg);
    }

    public DataCollectionException(String msg, Throwable e) {
        super(msg, e);
    }
}

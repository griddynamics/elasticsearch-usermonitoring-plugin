package com.griddynamics.elasticsearch.plugin.usermonitoring.limiter;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.rest.RestStatus;

public class UserRejectedException extends ElasticsearchException {
    public UserRejectedException(String msg, Object... args) {
        super(msg, args);
    }

    @Override
    public RestStatus status() {
        return RestStatus.TOO_MANY_REQUESTS;
    }
}

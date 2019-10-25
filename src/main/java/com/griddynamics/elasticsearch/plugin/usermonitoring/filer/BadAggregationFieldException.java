package com.griddynamics.elasticsearch.plugin.usermonitoring.filer;

import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationExecutionException;

public class BadAggregationFieldException extends AggregationExecutionException {
    public BadAggregationFieldException(String msg) {
        super(msg);
    }

    @Override
    public RestStatus status() {
        return RestStatus.TOO_MANY_REQUESTS;
    }
}

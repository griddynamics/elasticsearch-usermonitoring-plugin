package com.griddynamics.elasticsearch.plugin.usermonitoring.filer;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.tasks.Task;

import java.util.Collection;

import static java.util.Optional.ofNullable;

public class IdAggSkipActionFiler implements ActionFilter {
    @Override
    public int order() {
        return 0;
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void apply(Task task, String action, Request request,
                                                                                       ActionListener<Response> listener, ActionFilterChain<Request, Response> chain) {
        if (!recursiveCheckIdAggs(
                ofNullable(request)
                        .map(r -> r instanceof SearchRequest ? (SearchRequest) r : null)
                        .map(SearchRequest::source)
                        .map(SearchSourceBuilder::aggregations)
                        .map(AggregatorFactories.Builder::getAggregatorFactories).orElse(null)
                , listener)
        ) {
            chain.proceed(task, action, request, listener);
        }
    }

    private <Response extends ActionResponse> boolean recursiveCheckIdAggs(Collection<AggregationBuilder> aggBuilders,
                                                                           ActionListener<Response> listener) {
        if (aggBuilders == null || aggBuilders.isEmpty()) {
            return false;
        }
        for (AggregationBuilder aggBuilder : aggBuilders) {
            if (aggBuilder instanceof ValuesSourceAggregationBuilder
                    && "_id".equals(((ValuesSourceAggregationBuilder) aggBuilder).field())) {
                listener.onFailure(new BadAggregationFieldException("Unsupported aggregation by field \"_id\""));
                return true;
            } else {
                return recursiveCheckIdAggs(aggBuilder.getSubAggregations(), listener);
            }
        }
        return false;
    }
}

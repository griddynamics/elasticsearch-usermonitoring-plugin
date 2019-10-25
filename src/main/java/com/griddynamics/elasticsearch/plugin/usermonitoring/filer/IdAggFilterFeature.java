package com.griddynamics.elasticsearch.plugin.usermonitoring.filer;

import com.griddynamics.elasticsearch.plugin.usermonitoring.AbstractFeature;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;

import java.util.Collections;
import java.util.List;

public class IdAggFilterFeature extends AbstractFeature {

    private final IdAggSkipActionFiler idAggSkipActionFiler = new IdAggSkipActionFiler();

    public IdAggFilterFeature(String prefix, Settings settings) {
        super(prefix + "filter.id.aggregation", settings);
    }

    @Override
    protected boolean defaultEnabled() {
        return false;
    }

    @Override
    protected List<Setting<?>> getConfigurationSettings() {
        return Collections.emptyList();
    }

    public List<ActionFilter> getActionFilters() {
        if (isEnabled()) {
            return Collections.singletonList(idAggSkipActionFiler);
        } else {
            return Collections.emptyList();
        }
    }
}

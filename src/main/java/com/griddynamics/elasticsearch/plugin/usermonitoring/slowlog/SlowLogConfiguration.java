package com.griddynamics.elasticsearch.plugin.usermonitoring.slowlog;

class SlowLogConfiguration {
    private final boolean appendRoles;

    public SlowLogConfiguration(boolean appendRoles) {
        this.appendRoles = appendRoles;
    }

    public boolean isAppendRoles() {
        return appendRoles;
    }

}

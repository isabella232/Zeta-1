package com.ebay.dss.zds.model.alation;

public class AlationSchedule {
    private boolean enabled;
    private String cron_expression;
    private AlationScheduleConnection db_connection;

    public AlationSchedule() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron_expression() {
        return cron_expression;
    }

    public void setCron_expression(String cron_expression) {
        this.cron_expression = cron_expression;
    }

    public AlationScheduleConnection getDb_connection() {
        return db_connection;
    }

    public void setDb_connection(AlationScheduleConnection db_connection) {
        this.db_connection = db_connection;
    }
}
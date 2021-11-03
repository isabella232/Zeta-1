package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;
import com.google.gson.JsonObject;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public abstract class TagMetricEvent extends ZetaEvent {

    private Map<String, String> tags;
    private Map<String, Object> metrics;

    public TagMetricEvent() {
        this.tags = new HashMap<>();
        this.metrics = new HashMap<>();
    }

    public static class TrackedName {
        public static final String INTERPRETER_OPERATIONS="interpreter_operations";
    }

    public static TagMetricEvent newInstance(String name) {
        return new InfluxTagMetricEvent(name);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public String getTag(String tag) {
        return tags.get(tag);
    }

    public String setTag(String tag, String value) {
        return tags.put(tag, value);
    }

    public Object setMetric(String metric, Object value) {
        return metrics.put(metric, value);
    }

    public Object getMetric(String metric) {
        return metrics.get(metric);
    }

    private static class InfluxTagMetricEvent extends TagMetricEvent implements TaggedEvent.InfluxStorable {

        private String measurement;

        public InfluxTagMetricEvent(String measurement) {
            this.measurement = measurement;
        }

        @Override
        public String measurement() {
            return measurement;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject json = new JsonObject();
            json.addProperty("measurement", measurement());
            JsonObject tagsObject = new JsonObject();
            getTags().forEach(tagsObject::addProperty);
            json.add("tags", tagsObject);
            JsonObject metricsObject = new JsonObject();
            getMetrics().forEach((k, v) -> metricsObject.addProperty(k, v.toString()));
            json.add("metrics", metricsObject);
            return json;
        }

        @Override
        public @NotNull EventQueueIdentifier getIdentifier() {
            return EventQueueIdentifier.OPERATION;
        }
    }
}

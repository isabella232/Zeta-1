package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleModel;
import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.ZetaEvent;
import com.google.gson.JsonObject;

import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2019-06-21.
 */
public abstract class ZetaLifecycleEvent extends ZetaEvent {

    public EventQueueIdentifier getIdentifier() {
        return EventQueueIdentifier.LIFECYCLE;
    }

    protected LifeCycleModel cycle;

    public ZetaLifecycleEvent(LifeCycleModel cycle) {
        this.cycle = cycle;
    }

    @NotNull
    public LifeCycleModel getLifeCycle() {
        return this.cycle;
    }

    public static class ZetaLifecycleCheckEvent extends ZetaLifecycleEvent {

        public ZetaLifecycleCheckEvent(LifeCycleModel cycle) {
            super(cycle);
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            jo.addProperty("companionInterpreterGroup",
                    ((InterpreterGroup)getLifeCycle().getCompanionObject()).getGroupId());
            return jo;
        }
    }
}

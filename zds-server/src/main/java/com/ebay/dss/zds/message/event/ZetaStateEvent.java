package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.ZetaEvent;
import com.google.gson.JsonObject;

/**
 * Created by tatian on 2020-09-18.
 */
public abstract class ZetaStateEvent extends ZetaEvent {

  @Override
  public EventQueueIdentifier getIdentifier() {
    return EventQueueIdentifier.STATE;
  }

  protected String nt;
  protected String noteId;
  protected Interpreter interpreter;

  public ZetaStateEvent(String nt, String noteId, Interpreter interpreter) {
    this.nt = nt;
    this.noteId = noteId;
    this.interpreter = interpreter;
  }

  @Override
  public JsonObject toJsonObject() {
    JsonObject jo = new JsonObject();
    jo.addProperty("identifier", getIdentifier().name());
    jo.addProperty("eventClass", this.getClass().getSimpleName());
    JsonObject eventProperties = new JsonObject();
    eventProperties.addProperty("nt", nt);
    eventProperties.addProperty("noteId", noteId);
    eventProperties.addProperty("interpreterClass", interpreter.getClassName());
    eventProperties.addProperty("recordTime", recordTime.toString("yyyy-MM-dd HH:mm:ss"));
    eventProperties.addProperty("externalContext", getExternalContext().toString());
    jo.add("eventContext", eventProperties);
    return jo;
  }

  public Interpreter getInterpreter() {
    return interpreter;
  }

  public static class ZetaStatePersistEvent extends ZetaStateEvent {

    public ZetaStatePersistEvent(String nt, String noteId, Interpreter interpreter) {
      super(nt, noteId, interpreter);
    }
  }

  public static class ZetaStateUnPersistEvent extends ZetaStateEvent {

    public ZetaStateUnPersistEvent(String nt, String noteId, Interpreter interpreter) {
      super(nt, noteId, interpreter);
    }
  }
}

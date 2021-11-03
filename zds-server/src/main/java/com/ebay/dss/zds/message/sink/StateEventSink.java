package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.ebay.dss.zds.message.event.ZetaStateEvent;
import com.ebay.dss.zds.state.Recoverable;
import com.ebay.dss.zds.state.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2020-09-18.
 */
public class StateEventSink implements ZetaEventListener {

  private static final Logger logger = LoggerFactory.getLogger(StateEventSink.class);

  private StateManager stateManager;

  public StateEventSink(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  public void onEventReceived(ZetaEvent zetaEvent) {

    if (zetaEvent instanceof ZetaStateEvent) {
      Recoverable recoverable = (Recoverable) ((ZetaStateEvent) zetaEvent).getInterpreter();
      if (zetaEvent instanceof ZetaStateEvent.ZetaStatePersistEvent) {
        logger.info("Persisting interpreter state: {}", zetaEvent.toJson());
        recoverable.persist(stateManager);
        logger.info("Persisted");
      } else if (zetaEvent instanceof ZetaStateEvent.ZetaStateUnPersistEvent) {
        logger.info("Destroying interpreter state: {}", zetaEvent.toJson());
        recoverable.destroy(stateManager);
        logger.info("Destroyed");
      }
    }

  }
}

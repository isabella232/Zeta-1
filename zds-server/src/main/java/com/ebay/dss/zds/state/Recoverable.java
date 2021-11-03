package com.ebay.dss.zds.state;


/**
 * Created by tatian on 2020-08-19.
 */
public interface Recoverable {

  default boolean persist(StateManager stateManager) {
    synchronized (this) {
      StateSnapshot stateSnapshot = createSnapshot();
      if (stateSnapshot != null && !stateSnapshot.equals(StateSnapshot.EMPTY)) {
        return stateManager.storeStateSnapshot(recoverKey(), stateSnapshot);
      } else return false;
    }
  }

  default boolean recover(StateManager stateManager) {
    synchronized (this) {
      StateSnapshot stateSnapshot = stateManager.findStateSnapshot(recoverKey());
      if (stateSnapshot != null && !stateSnapshot.equals(StateSnapshot.EMPTY)) {
        stateSnapshot.setKey(recoverKey());
        stateSnapshot.setStateManager(stateManager);
        return doRecover(stateSnapshot);
      } else {
        return false;
      }
    }
  }

  default void destroy(StateManager stateManager) {
    synchronized (this) {
      stateManager.destroyStateSnapshot(recoverKey());
    }
  }

  StateSnapshot createSnapshot();

  boolean doRecover(StateSnapshot stateSnapshot);

  String recoverKey();

}

import { State } from './state';
import { StateController, controllerMap } from './state-controller';

export class QueryState {
  stateController: StateController;

  /** state controller map */
  controllerMap: Record<State, StateController> = controllerMap;

  constructor(state = State.WAITING) {
    /** init status */
    this.stateController = this.controllerMap[state];
  }
  submit() {
    if (this.stateController.submit) {
      this.stateController = this.controllerMap[State.SUBMITTED];
      return true;
    }
    return false;
  }

  onWaiting() {
    if (this.stateController.onWaiting) {
      this.stateController = this.controllerMap[State.WAITING];
      return true;
    }
    return false;
  }

  onRun() {
    if (this.stateController.onRun) {
      this.stateController = this.controllerMap[State.RUNNING];
      return true;
    }
    return false;
  }
  onProgress() {
    if (this.stateController.onProgress) {
      this.stateController = this.controllerMap[State.RUNNING];
      return true;
    }
    return false;
  }
  onCancel() {
    if (this.stateController.onCancel) {
      this.stateController = this.controllerMap[State.CANCELED];
      return true;
    }
    return false;
  }
  onError() {
    if (this.stateController.onError) {
      this.stateController = this.controllerMap[State.ERROR];
      return true;
    }
    return false;
  }
  onSuccess() {
    if (this.stateController.onSuccess) {
      this.stateController = this.controllerMap[State.SUCCESS];
      return true;
    }
    return false;
  }

  toString() {
    return this.stateController.toString();
  }

  valueOf() {
    return this.stateController.valueOf();
  }

}

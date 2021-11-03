import { StateController, controllerMap } from './state-controller';
import { State } from './state';

export class NotebookJobStatus {
  stateController: StateController;
  controllerMap: Record<State, StateController> = controllerMap;

  constructor(state = State.SUBMITTED) {
    this.stateController = this.controllerMap[state];
  }

  submit() {
    if (this.stateController.submit) {
      this.stateController = this.controllerMap[State.SUBMITTED];
      return true;
    }
    return false;
  }
  onSubmitting() {
    if (this.stateController.onSubmitting) {
      this.stateController = this.controllerMap[State.SUBMITTING];
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
  onRunning() {
    if (this.stateController.onRunning) {
      this.stateController = this.controllerMap[State.RUNNING];
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
  onError() {
    if (this.stateController.onError) {
      this.stateController = this.controllerMap[State.ERROR];
      return true;
    }
    return false;
  }
  onCanceled() {
    if (this.stateController.onCanceled) {
      this.stateController = this.controllerMap[State.CANCELED];
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

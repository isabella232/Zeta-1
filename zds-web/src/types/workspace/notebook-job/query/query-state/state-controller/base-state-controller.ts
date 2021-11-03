import { QueryAction } from '../query-action';
import { State } from '../state';

export default abstract class StateController implements QueryAction {
  readonly currentState: State;

  submit = false;
  onWaiting = false;
  onRun = false;
  onProgress = false;
  onCancel = false;
  onError = false;
  onSuccess = false;


  toString() {
    return this.currentState.toString();
  }

  valueOf() {
    return this.currentState.toString();
  }

}

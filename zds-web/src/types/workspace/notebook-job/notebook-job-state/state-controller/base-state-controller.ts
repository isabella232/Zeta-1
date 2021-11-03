import { State } from '../state';
import { JobController } from './job-controller';

export default abstract class StateController implements JobController{
  readonly currentState: State;

  submit = false;
  onSubmitting = false;
  onWaiting = false;
  onRunning = false;
  onSuccess = false;
  onError = false;
  onCanceled = false;

  toString() {
    return this.currentState.toString();
  }

  valueOf() {
    return this.currentState.toString();
  }

}

import StateController from './base-state-controller';
import { State } from '../state';

export default class SubmittedStateController extends StateController {
  readonly currentState = State.SUBMITTED;

  submit = true;
  onSubmitting = true;
  onWaiting = true;
  onRunning = true;
  onSuccess = true;
  onError = true;
  onCanceled = true;
}

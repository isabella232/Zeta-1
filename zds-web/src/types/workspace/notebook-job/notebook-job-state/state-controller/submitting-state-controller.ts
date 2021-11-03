import StateController from './base-state-controller';
import { State } from '../state';

export default class SubmittingStateController extends StateController {
  readonly currentState = State.SUBMITTING;

  onSubmitting = true;
  onWaiting = true;
  onRunning = true;
  onSuccess = true;
  onError = true;
  onCanceled = true;
}

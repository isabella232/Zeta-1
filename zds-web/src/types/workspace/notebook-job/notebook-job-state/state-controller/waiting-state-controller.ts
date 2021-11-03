import StateController from './base-state-controller';
import { State } from '../state';

export default class WaitingStateController extends StateController {
  readonly currentState = State.WAITING;

  onWaiting = true;
  onRunning = true;
  onSuccess = true;
  onError = true;
  onCanceled = true;
}

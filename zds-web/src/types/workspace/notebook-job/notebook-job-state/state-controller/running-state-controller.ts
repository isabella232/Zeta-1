import StateController from './base-state-controller';
import { State } from '../state';

export default class RunningStateController extends StateController {
  readonly currentState = State.RUNNING;

  onRunning = true;
  onSuccess = true;
  onError = true;
  onCanceled = true;
}

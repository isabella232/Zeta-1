import StateController from './base-state-controller';
import { State } from '../state';

export default class WaitingStateController extends StateController {
  currentState = State.WAITING;

  onWaiting = true;
  onRun = true;
  onProgress = true;
  onCancel = true;
  onError = true;
  onSuccess = true;
}

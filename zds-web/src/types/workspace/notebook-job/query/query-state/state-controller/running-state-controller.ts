import StateController from './base-state-controller';
import { State } from '../state';

export default class RunningStateController extends StateController {
  currentState = State.RUNNING;

  onProgress = true;

  onCancel = true;

  onError = true;

  onSuccess = true;
}

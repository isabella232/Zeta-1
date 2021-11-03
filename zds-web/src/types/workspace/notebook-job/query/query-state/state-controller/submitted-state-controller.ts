import StateController from './base-state-controller';
import { State } from '../state';

export default class SubmittedStateController extends StateController {
  currentState = State.SUBMITTED;

  onWaiting = true;

  onRun = true;

  onProgress = true;

  onCancel = true;

  onError = true;

  onSuccess = true;
}

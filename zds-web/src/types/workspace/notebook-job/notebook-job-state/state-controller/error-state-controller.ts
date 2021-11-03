import StateController from './base-state-controller';
import { State } from '../state';

export default class RunningStateController extends StateController {
  readonly currentState = State.ERROR;

  onSuccess = true;
}

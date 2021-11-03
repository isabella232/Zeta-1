import StateController from './base-state-controller';
import { State } from '../state';

export default class SuccessStateController extends StateController {
  currentState = State.SUCCESS;

  onSuccess = true;
}

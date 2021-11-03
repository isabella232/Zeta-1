import StateController from './base-state-controller';
import { State } from '../state';

export default class SuccessStateController extends StateController {
  readonly currentState = State.SUCCESS;
}

import StateController from './base-state-controller';
import { State } from '../state';

export default class ErrorStateController extends StateController {
  currentState = State.ERROR;
}

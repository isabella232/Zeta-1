import StateController from './base-state-controller';
import { State } from '../state';

export default class CanceledStateController extends StateController {
  currentState = State.CANCELED;
}

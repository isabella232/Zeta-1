
import SubmittedStateController from './submitted-state-controller';
import StateController from './base-state-controller';
import WaitingStateController from './waiting-state-controller';
import RunningStateController from './running-state-controller';
import SuccessStateController from './success-state-controller';
import CanceledStateController from './canceled-state-controller';
import ErrorStateController from './error-state-controller';
import { State } from '../state';

const controllerMap = {
  [State.SUBMITTED]: new SubmittedStateController(),
  [State.WAITING]: new WaitingStateController(),
  [State.RUNNING]: new RunningStateController(),
  [State.SUCCESS]: new SuccessStateController(),
  [State.CANCELED]: new CanceledStateController(),
  [State.ERROR]: new ErrorStateController(),
};

export {
  StateController,
  controllerMap
};

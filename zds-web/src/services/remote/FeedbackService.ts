import DssAPI from '@/services/remote/dss-api-remote-service';
import { Feedback, FDType } from '@/types/workspace/feedback.internal';

import config from '@/config/config';
const baseUrl = config.zeta.nodesvc;

export default class FeedbackService extends DssAPI {

  addFeedback (statementId: number, param: Feedback){
    const type = param.feedbackType;
    return this.put(`${baseUrl}feedback/${type}/${statementId}`, param);
  }
  getFeedback (type: FDType, statementId: number){
    return this.get(`${baseUrl}feedback/${type}/${statementId}`);
  }

}

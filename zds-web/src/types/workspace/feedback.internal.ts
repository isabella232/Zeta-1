
export enum FDType {
  query,
  dv,
  dm
}

export interface Feedback {
  usreId: string;
  comments: string;
  feedbackScore: number;
  feedbackType: FDType;
  context: number;
}


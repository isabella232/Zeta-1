export interface JobController {
  submit: boolean;
  onSubmitting: boolean;
  onWaiting: boolean;
  onRunning: boolean;
  onSuccess: boolean;
  onError: boolean;
  onCanceled: boolean;
}

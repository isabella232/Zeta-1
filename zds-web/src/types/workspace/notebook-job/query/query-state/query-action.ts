
export interface QueryAction {
  submit: boolean;

  onWaiting: boolean;

  onRun: boolean;

  onProgress: boolean;

  onCancel: boolean;

  onError: boolean;

  onSuccess: boolean;

}

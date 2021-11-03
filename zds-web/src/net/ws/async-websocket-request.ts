export interface AsyncWSRequest {
  id: string;
  opAlias: string;
  resolver: Function;
  rejecter: Function;
  mapping: Dict<string>;
}
export interface AsyncWSMessageOption {
  opAlias: string;
  mapping: Dict<string>;
  timeout?: number;
}
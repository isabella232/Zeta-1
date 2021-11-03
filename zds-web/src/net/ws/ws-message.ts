import 'reflect-metadata';

export const WSMessageMetadataKey = 'wsmessage:options';
export const WSMessageMetadataExecutorKey = 'wsmessage:executor';

export interface WSMessageOptions {
  isError?: boolean;
  topic?: string;
  op?: string;
  /** async response required */
  alias?: string;
  mappingPath?: Dict<string>;
  ignoreTracking?: boolean;
}

export function WSMessage(option: WSMessageOptions = { isError: false }) {
  return function(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    const $op = option.op || propertyKey;
    const executor: Function = target[propertyKey];
    Reflect.defineMetadata(WSMessageMetadataKey, option, target, $op);
    Reflect.defineMetadata(WSMessageMetadataExecutorKey, executor, target, $op);
  };
}
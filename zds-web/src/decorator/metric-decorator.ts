import { Tags } from "@/plugins/logs/tag";

export interface MetricOptions {
    name: string,
    value?: number
    tags?: Tags
}
export function CountMetric(option: MetricOptions) {
    return function(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        console.log(target, propertyKey, descriptor)
    }
}
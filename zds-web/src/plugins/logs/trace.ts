export enum EventType {
    INTERNAL_ACTION      = 'ACTION',
    PAGE_VIEW            = 'PAGE_VIEW',
    USER_CLICK           = 'USER_CLICK'
}
export class Packet {
    event: EventType;
    name: string;
    desc: string;
    constructor(event: EventType, name: string, desc: string) {
        this.event = event;
        this.name = name;
        this.desc = desc;
    }
}
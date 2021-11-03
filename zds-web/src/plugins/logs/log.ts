export type MessageType = 'log' | 'trace';
export class Message {
    title: string
    content: string
    type: MessageType
    constructor(type: MessageType, title: string, content: string = '' ) {
        this.type = type;
        this.title = title;
        this.content = content;
    }
}
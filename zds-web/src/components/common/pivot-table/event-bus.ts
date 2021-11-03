import { Vue } from 'vue-property-decorator';

class CEventBus extends Vue {
}

export const EventBus = new CEventBus();

export const EVENT_KEY = {
  SET_CURRENT: 'set_current',
  SET_ATTRIBUTES: 'set_attributes',
};

export interface CurrentEvent {
  source: string;
  current: string;
}

export interface AttributesEvent {
  source: string;
  value: string[];
}

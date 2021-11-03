<template>
<div>
  <draggable v-model="attributes" @change='onChange' :options="{group:'attribute'}" class='drag-group'>
    <div v-for="element in attributes" :key="element" class='drag-item'>{{element}}</div>
  </draggable>

  <draggable v-model="attributes2" @start="drag=true" @end="drag=false" :options="{group:'attribute'}" class='drag-group'>
    <div v-for="element in attributes2" :key="element" class='drag-item'>{{element}}</div>
  </draggable>

</div>
</template>

<script lang='ts'>
import { Vue, Component, Prop } from 'vue-property-decorator';
import draggable from 'vuedraggable';
import { EventBus, EVENT_KEY } from './event-bus';

@Component({
  components: {draggable},
})
export default class UnusedAttributes extends Vue {
  @Prop() attributesIn!: string[];
  @Prop() name!: string;
  attributes: string[] = [];

  attributes2: string[] = ['1', '2'];

  constructor() {
    super();
  }

  beforeMount() {
    this.attributes = this.attributesIn;
  }

  onChange( ) {
    EventBus.$emit(EVENT_KEY.SET_ATTRIBUTES, {source: this.name, value: this.attributes});
  }
}
</script>
<style>
.drag-group {
  flex-direction: row;
  min-width:10px;
  min-height: 10px;
}
.drag-item {
  display: inline-flex;
  text-align: left;
  border: 1px solid #a2b1c6;
  border-radius: 4px;
  box-sizing: border-box;
  background: #fff;
  margin: 2px 5px;
  padding: 2px 5px;
  cursor: pointer;
}
</style>
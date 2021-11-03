<template>
  <div
    ref="mount"
    :class="{'read-only-code-mirror': readOnly}"
    class="code-display"
  />
</template>

<script lang="ts">
/**
 * Component <CodeDisplay>. A readonly highlighted SQL displayer.
 *
 * @prop code SQL code to display
 * @prop type Code type. (unused, only SQL supported)
 */
import { Component, Vue, Prop, Watch, Emit } from 'vue-property-decorator';
import CodeMirror, { EditorConfiguration } from 'codemirror';
import '@/styles/codemirror.my.scss';
import 'codemirror/mode/sql/sql';
import 'codemirror/addon/runmode/runmode';
import ResizeObserver from 'resize-observer-polyfill';
@Component({
  components: {
    /* Blank */
  },
})

export default class CodeDisplay extends Vue {

  @Prop() value: string;
  get code() {
    return this.value;
  }
  @Prop({ default: 'sql' }) type: string;
  @Prop({ default: true}) readOnly: boolean;
  @Prop() options?: CodeMirror.EditorConfiguration;
  cm: CodeMirror.Editor;

  obsrv: ResizeObserver;
  mounted() {
    const options = this.getCMOption(this.options);
    const cm = this.cm = CodeMirror(this.$el as HTMLElement, options);
    this.cm.on('change', () => {
      this.onValueChange(this.cm.getValue());
    });
    this.obsrv = new ResizeObserver(entries => {
      const resizeObject = entries.find(entry => entry.target === this.$el);
      if (resizeObject) {
        cm.refresh();
      }
    });
    this.obsrv.observe(this.$el);

  }
  destroyed() {
    this.obsrv.unobserve(this.$el);
  }
  activated() {
    if (!this.$data.scrollInfo) return;
    const { left, top } = this.$data.scrollInfo;
    this.scrollTo(left, top);
  }

  @Watch('code')
  onCodeChange(code: string, old_code: string) {
    const isClear = !code;
    if(this.readOnly || isClear){
      this.cm.setValue(code);
    }
  }
  @Watch('option', {deep: true})
  onOptionChange(o: CodeMirror.EditorConfiguration) {
    const options = this.getCMOption(o);
    this.setOptions(options);
  }
  @Emit('input')
  onValueChange(val: string) {

  }
  private scrollTo(left = 0, top = 0) {
    this.cm.scrollTo(left, top);
  }
  private getCMOption(o?: CodeMirror.EditorConfiguration) {
    return Object.assign({
      readOnly: this.readOnly,
      value: this.code,
      mode: 'text/x-mysql',
      lineNumbers: true,
    }, o ? o : {});
  }
  private setOptions(options: any) {
    for(const key in options) {
      this.cm.setOption(key as keyof EditorConfiguration, options[key]);
    }
  }
}
</script>

<style lang="scss" scoped>
.code-display {
  height:100%;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  /deep/ .CodeMirror {
    flex-direction: column;
    display: flex;
    flex-grow: 1;
    min-height: 24px;
    .CodeMirror-scroll {
      flex-grow: 1;
    }
  }
}
</style>


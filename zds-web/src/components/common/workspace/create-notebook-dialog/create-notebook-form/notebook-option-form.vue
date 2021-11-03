<template>
  <el-form
    ref="form"
    label-position="left"
    label-width="130px"
    :model="subForm"
  >
    <el-form-item label="Interpreter">
      <template slot="label">
        <span v-if="!connected">
          Interpreter
        </span>

        <el-tooltip
          v-if="connected"
          content="Interpreter cannot be changed when connected"
          placement="bottom"
          effect="light"
        >
          <span>
            Interpreter
          </span>
        </el-tooltip>
      </template>
      <el-select
        v-model="subForm.interpreter"
        :disabled="connected"
        class="interpreter-selector"
      >
        <el-option
          v-for="(interpreter,i) in interpreterOptions"
          :key="i"
          :value="interpreter"
          :label="interpreter"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="Notebook Type">
      <div class="type-radio-group">
        <div
          v-if="!disableSole"
          class="type-radio"
        >
          <div class="selector-left">
            <label
              for="solo"
              class="solo"
              :class="{'disabled': disableSole}"
            >
              <div class="code" />
            </label>
            <div
              class="selector"
              :class="{'selected': subForm.noteType == 'solo'}"
            >
              <input
                id="solo"
                v-model="subForm.noteType"
                style="display: none;"
                type="radio"
                value="solo"
                :disabled="disableSole"
              >
              <label for="solo">
                <span
                  for="solo"
                  class="radio-inner"
                />SOLO</label>
            </div>
          </div>
          <div class="selector-desc">
            Single editor and results panel. Multiple SQL statements can be run. For SQL languages only.
          </div>
        </div>
        <div
          v-if="!disableBand"
          class="type-radio"
        >
          <div class="selector-left">
            <label
              for="stacked"
              class="stacked"
              :class="{'disabled': disableBand}"
            >
              <div class="code" />
              <div class="code" />
              <div class="code" />
            </label>
            <div
              class="selector"
              :class="{'selected': subForm.noteType == 'stacked'}"
            >
              <input
                id="stacked"
                v-model="subForm.noteType"
                style="display: none;"
                type="radio"
                :disabled="disableBand"
                value="stacked"
              >
              <label for="stacked">
                <span
                  for="stacked"
                  class="radio-inner"
                />STACKED</label>
            </div>
          </div>
          <div class="selector-desc">
            Multiple editor and results panels in stack. Each panel can use a different coding language.
          </div>
        </div>
      </div>
    </el-form-item>
    <el-form-item
      v-if="hasPlatfrom"
      label="Platform"
    >
      <span slot="label">
        Platform
        <el-tooltip
          v-if="hasDisabledPlatforms()"
          content="Bottom center"
          placement="bottom"
          effect="light"
        >
          <div slot="content">
            please follow
            <a
              target="_blank"
              href="https://wiki.vip.corp.ebay.com/x/l00VIg"
            >wiki</a>
            to access Apollo Reno
          </div>
          <i class="el-icon-question" />
        </el-tooltip>
      </span>
      <notebook-platform-selector
        v-model="subForm.platform"
        :option="option"
      />
    </el-form-item>
    <div
      v-if="hasDescription"
      class="comment"
    >
      {{ option.description }}
    </div>
  </el-form>
</template>

<script lang="ts">
import _ from 'lodash';
import NotebookPlatformSelector from './notebook-platform-selector.vue';
import { Component, Vue, Prop, Watch, Inject, Emit } from 'vue-property-decorator';
import { noteOptions, noteOptionByInterpreter, Interpreter, NoteOption, NoteType, interpreterOptions } from './note-options';
import { NoteExtra } from './note-form';

@Component({
  components: {
    NotebookPlatformSelector,
  },
  inheritAttrs: false,
})
export default class NotebookOptionForm extends Vue {
  @Inject()
  isDisabledPlatform: (platform: string, noteType: NoteType) => boolean;

  @Prop()
  value: NoteExtra;

  @Prop({ default: false })
  edit: boolean;

  @Prop({ default: false})
  connected: boolean;

  subForm: NoteExtra = {
    interpreter: 'Spark SQL',
    noteType: 'solo',
    platform: '',
  };

  @Watch('value', {immediate: true, deep: true })
  onValueChange(newVal: NoteExtra, oldVal: NoteExtra) {
    if (!_.isEmpty(newVal) && !_.isEqual(newVal, oldVal)) {
      this.$set(this, 'subForm', newVal);
    }
  }
  get interpreterOptions() {
    if (!this.edit) {
      return interpreterOptions;
    } else {
      return _.chain(noteOptions)
        .filter(option => option.type === this.subForm.noteType)
        .sortBy('interpreter')
        .map(option => option.interpreter)
        .value();
    }
  }
  get disableSole() {
    // Edit mode
    if (this.edit && this.subForm.noteType === 'stacked') return true;
    const interpreter = this.subForm.interpreter;
    const options = noteOptionByInterpreter(interpreter).find(option => option.type === 'solo');
    return !Boolean(options);
  }
  get disableBand() {
    // Edit mode
    if (this.edit && this.subForm.noteType === 'solo') return true;

    const interpreter = this.subForm.interpreter;
    const options = noteOptionByInterpreter(interpreter).find(option => option.type === 'stacked');
    return !Boolean(options);
  }

  get option(): NoteOption{
    return noteOptionByInterpreter(this.subForm.interpreter)
      .find(option => option.type === this.subForm.noteType) as NoteOption;
  }

  get hasPlatfrom() {
    return Boolean(this.option && this.option.platform && !_.isEmpty(this.option.platform));
  }

  get hasDescription() {
    return Boolean(this.option && this.option.description);
  }


  hasDisabledPlatforms() {
    const platforms = _.keys(this.option.platform);
    const type = this.subForm.noteType;
    const result = _.chain(platforms)
      .map(p => this.isDisabledPlatform(p, type))
      .reduce((res, val) => {
        if (val) {
          res = true;
        }
        return res;
      }, false)
      .value();
    return result;
  }
  setDefaultNoteType(interpreter: Interpreter) {
    const options = noteOptionByInterpreter(interpreter);
    if (options && options.length > 0) {
      this.subForm.noteType = options[0].type;
    }
  }
  @Watch('subForm.interpreter')
  onFormInterpreterChange(newVal: Interpreter) {
    this.setDefaultNoteType(newVal);
  }
  @Watch('subForm', { deep: true })
  onFormChange(newVal: NoteExtra) {
    if (_.isEmpty(newVal)) return;
    this.onOptionsChange(newVal);
  }

  @Emit('input')
  onOptionsChange(noteOptions: NoteExtra) {
    // save to localstorage
    localStorage.setItem('createNote.interpreter', noteOptions.interpreter);
    localStorage.setItem('createNote.noteType', noteOptions.noteType);
    localStorage.setItem('createNote.platform', noteOptions.platform);
    return noteOptions;
  }

}
</script>

<style lang="scss" scoped>
.comment {
  color: #999;
  font-size: 12px;
  line-height: 30px;
}
/deep/ .interpreter-selector {
  width: 100% !important;
}
.type-radio-group {
  display: flex;
  .type-radio {
    margin-right: 20px;
    display: flex;
    .selector-left {

    }
    .selector-desc {
      color: #999;
      width: 120px;
      font-size: 12px;
      line-height: initial;
      word-break: break-word;
      padding: 5px 0px;
      margin: 0 10px;
    }
    .solo,
    .stacked {
      display: flex;
      flex-direction: column;
      padding: 10px;
      border: 1px solid #ebebeb;
      width: 80px;
      height: 80px;
      border-radius: 5px;
      .code {
        background-color: #e8f3fe;
        flex-grow: 1;
        margin-bottom: 10px;
        &:nth-last-child(1) {
          margin-bottom: 0;
        }
      }
      &.disabled {
        .code {
          background-color: #DDD;
        }
        .radio-inner {
          cursor: not-allowed;
        }
      }
    }
    .selector {
      width: 100%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      .radio-inner {
        border: 1px solid #dcdfe6;
        border-radius: 100%;
        width: 14px;
        height: 14px;
        background-color: #fff;
        position: relative;
        cursor: pointer;
        display: inline-block;
        box-sizing: border-box;
        margin-right: 5px;
        &::after {
          width: 4px;
          height: 4px;
          border-radius: 100%;
          background-color: #fff;
          content: "";
          position: absolute;
          left: 50%;
          top: 50%;
          transform: translate(-50%,-50%) scale(0);
          transition: transform .15s ease-in;
        }
      }
      &.selected {
        color: #409eff;
        .radio-inner {
          border-color: #409eff;
          background: #409eff;
          &::after {
            transform: translate(-50%,-50%) scale(1);
          }
        }
      }
    }
  }
}
</style>

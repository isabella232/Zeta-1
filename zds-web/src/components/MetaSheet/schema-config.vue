<template>
  <div class="schema-container">
    <el-table :data="data" max-height="300" :row-class-name="getRowClass">
      <el-table-column prop="column" width="150">
        <template slot="header" slot-scope="scope">
          <span :class="{require: editable}">Column Name</span>
        </template>
        <template slot-scope="scope">
          <div v-if="scope.row.editable" :class="{'is-error': !columnValid(scope.row, 'column')}">
            <el-input  v-model.trim="scope.row.column" placeholder="column name"></el-input>
            <span class="error-info">{{columnErrorMsg(scope.row)}}</span>
          </div>
          <span v-else>{{scope.row.column}}</span>
        </template>
      </el-table-column>
      <el-table-column prop="type" width="150">
        <template slot="header" slot-scope="scope">
          <span :class="{require: editable}">Type</span>
        </template>
        <template slot-scope="scope">
          <div v-if="scope.row.editable" :class="{'is-error': !columnValid(scope.row, 'type')}">
            <el-select v-model="scope.row.type" @change="typeChange">
                <el-option
                v-for="(option) in initType"
                :key="option.value"
                :label="option.value"
                :value="option.value"
                ></el-option>
            </el-select>
            <span class="error-info">This field is required</span>
          </div>
          <span v-else>{{scope.row.type}}</span>
        </template>
      </el-table-column>
      <el-table-column prop="length" label="Length">
        <template slot-scope="scope">
          <div v-if="scope.row.editable" :class="{'is-error': !columnValid(scope.row, 'length')}">
            <el-input v-if="scope.row.editable" v-model.trim="scope.row.length" :placeholder="placeholder(scope.row.type)" :disabled="disbaleLength(scope.row)"></el-input>
            <span class="error-info">{{lengthErrorMsg}}</span>
          </div>
          <span v-else>{{scope.row.length}}</span>
        </template>
      </el-table-column>
      <el-table-column prop="desc" width="200">
        <template slot="header" slot-scope="scope">
          <span :class="{require: editable}">Description</span>
        </template>
        <template slot-scope="scope">
          <div v-if="scope.row.editable" :class="{'is-error': !columnValid(scope.row, 'desc')}">
            <el-input v-model="scope.row.desc" placeholder=""></el-input>
            <span class="error-info">This field is required</span>
          </div>
          <span v-else>{{scope.row.desc}}</span>
        </template>
      </el-table-column>
      <el-table-column prop="nullValue" label="Null">
        <template slot-scope="scope">
          <el-checkbox v-if="scope.row.editable" v-model="scope.row.nullValue"></el-checkbox>
          <span v-else>{{scope.row.nullValue ? 'Y' : 'N'}}</span>
        </template>
      </el-table-column>
      <el-table-column prop="primaryKey" label="Unique Key" width="130">
        <template slot-scope="scope">
          <el-checkbox v-if="scope.row.editable" v-model="scope.row.primaryKey" :disabled="disbaleUniqKey(scope.row)"></el-checkbox>
          <span v-else>{{scope.row.primaryKey ? 'Y' : 'N'}}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="editable && data.length > 1" width="50">
        <template slot-scope="scope">
          <span><i class="el-icon-close" @click="deleteRow(scope.$index)"></i></span>
        </template>
      </el-table-column>
    </el-table>
    <div class="add-new" v-if="editable">
      <span @click="addNewRow()"><i class="el-icon-circle-plus-outline"></i>Add new row</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import { MetaConfigTableRow, Mode, MODE } from '@/types/meta-sheet';
import _ from "lodash";
import Util, {_ID_, _VERSION_, BLACKLIST} from './util';
const errorMsgOption = {
  'int': 'Length should between 1 and 11',
  'bigint': 'Length should between 1 and 20',
  'varchar': 'Length should between 1 and 255',
  // 'double':'Format is M,D. eg: 16,4',
  'decimal':'Format is M,D. eg: 16,4',
}
@Component({
  components: {
  }
})
export default class MetaSchemeConfigVue extends Vue {
  @Prop() data: Array<MetaConfigTableRow>;
  @Prop() mode: Mode; // EDIT or ADD
  initType:any = [];
  lengthErrorMsg = '';
  constructor() {
    super();
    const types = _.filter(BLACKLIST, d => [_ID_,_VERSION_].indexOf(d) == -1);
    this.initType = _.map(types, d => {
        return {value: d}
    })
  }
  get editable() {
    return this.mode == MODE.EDIT || this.mode == MODE.ADD;
  }

  placeholder(type) {
    let placeholder = '';
    switch(type) {
      case 'varchar':
        placeholder = '255';
        break;
      case 'int':
        placeholder = '11';
        break;
      case 'bigint':
        placeholder = '20';
        break;
      // case 'double':
      case 'decimal':
        placeholder = '16,4';
        break;
      default:
        placeholder = '';
    }
    return placeholder;
  }

  columnErrorMsg(row: MetaConfigTableRow) {
    if(row.column && BLACKLIST.map(d => d.toLowerCase()).indexOf(row.column.toLowerCase()) != -1) {
      return `${row.column} not allowed`;
    } else {
      return 'This field is required';
    }

  }

  addNewRow() {
    this.$emit("add-row");
    this.$nextTick(() => {
      let $container: HTMLElement | null = document.querySelector(".meta-config-dialog .el-table .el-table__body-wrapper");
      if($container) {
        $container.scrollTop = $container.scrollHeight - $container.offsetHeight;
      }
    });
  }

  deleteRow(index: number) {
    this.$emit("delete-row", index);
  }

  validate() {
    this.$emit("validate");
    let emptyRow = _.find(this.data, row => Util.isInvalidRow(row));
    if(!emptyRow) {
      return true;
    } else {
      return false;
    }
  }

  getRowClass({row, rowIndex}) {
    if(row.validate) {
      let result = Util.isInvalidRow(row);
      return result ? 'error-row' : '';
    } else {
      return '';
    }
  }

  columnValid(row, columnName: 'column' | 'type' | 'desc' | 'length') {
    if(row.validate) {
      if(Util.isValid(row, columnName)) {
        return true;
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  typeChange(val: string){
    this.lengthErrorMsg = errorMsgOption[val] || '';
  }

  disbaleLength(row:MetaConfigTableRow) {
    if(['string', 'date', 'timestamp'].indexOf(row.type) != -1) {
      row.length = '';
      return true;
    }
    return false;
  }

  disbaleUniqKey(row:MetaConfigTableRow) {
    if(['string'].indexOf(row.type) != -1) {
      row.primaryKey = false;
      return true;
    }
    return false;
  }

}
</script>

<style lang="scss" scoped>
$blue: #569ce1;
$p10: 10px;
$margin2x: 20px;
$errorColor: #f56c6c;
.schema-container {
  .add-new {
    margin-top: $p10;
    color: $blue;
    cursor: pointer;
    i {
      margin-right: $p10;
    }
  }

  span.require::after {
    content: '*';
    color: $errorColor;
    margin-left: $p10 / 2;
    font-size: 16px;
  }

  /deep/ .error-row {
    height: 70px;
  }

  .error-info {
    display: none;
    position: absolute;
    top: 50px;
    padding: 0 10px;
    color: $errorColor;
  }

  .is-error {
    .error-info {
      display: block;
    }
    /deep/ .el-input__inner {
      border-color: $errorColor;
    }
  }
}
</style>

<template>
    <div>
        <div class="head-div">
            <p class="dataSet-label">Reference</p>
            <el-popover placement="bottom" width="400" trigger="click">
                <div>
                    <el-input v-model="refTitle" style="width: 400px;" ref="input" class="link">
                        <template slot="prepend">title:</template>
                    </el-input>
                    <el-input v-model="refLink" style="width: 400px;" ref="input" class="link">
                      <template slot="prepend">link:</template>
                      <el-button slot="append" @click="addReferences">Add</el-button>
                    </el-input>
                </div>
                <el-button type="text" slot="reference" class="owner-btn" title="Add Reference" v-show="isOwner && isEdit">
                    <i class="zeta-icon-add"> Add</i>
                </el-button>
            </el-popover>
        </div>
        <div>
            <div v-for="ref in refArr" :key="ref.title" class="user-item-div">
                <span class="owner-btn" v-show="isOwner && isEdit">
                    <el-popover
                        placement="top"
                        width="180"
                        v-model="ref.visible">
                        <p>Are you sure to delete this?</p>
                        <div style="text-align: right; margin: 0; margin-top:5px;">
                        <el-button size="mini" type="text" @click="ref.visible = false">cancel</el-button>
                        <el-button type="primary" size="mini" @click="removeReferences(ref.title)">confirm</el-button>
                        </div>
                        <el-button type="text" class="owner-btn" title="Remove Reference" slot="reference" @click="ref.visible = true">
                            <i class="el-icon-minus"></i>
                        </el-button>
                    </el-popover>
                </span>
                <div class="nav-item-icon">
                    <a v-click-metric:METADATA_BROWSE="{name: 'reference', domain: domain, subDomain: subDomain, title:ref.title}" :href="ref.link" target="view_window">{{ ref.title }}</a>
                </div>
            </div>
            <div v-if="!hasRef" class="no-data-div">No content yet</div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject, Watch } from "vue-property-decorator";
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import Util from "@/services/Util.service";
import _ from 'lodash';
import $ from 'jquery';

@Component({
  components: {
    
  }
})
export default class DatasetsReference extends Vue {
    @Inject('doeRemoteService')
    doeRemoteService: DoeRemoteService;

    @Prop() isOwner: any;
    @Prop() domain: any;
    @Prop() subDomain: any;
    @Prop() reference: any;
    @Prop() isEdit: any;

    debounce: number = 500;
    // search func
    userArr: any = [];

    // reference func
    refTitle: any = "";
    refLink: any = "";
    refArr: any = this.reference;

    get hasRef() {
        return _.size(this.refArr) > 0;
    }

    // references fun
    addReferences() {
        if (_.isEmpty(this.refTitle)) {
            this.$message.error("Please input title");
        return;
        }
        const findIndex = _.findIndex(this.refArr, (v: any) => { return v.title == this.refTitle });
        if (findIndex < 0) {
            const ref: any = {title: this.refTitle, link: this.refLink};
            this.refArr.push(ref);
            this.refTitle = "";
            this.refLink = "";
            const conf: any = { reference: JSON.stringify(this.refArr) };
            this.$emit("submit-conf", conf);
        }else {
            this.$message.error("Duplicate title");
            return;
        }
    }

    removeReferences(title: any) {
        let refArr = _.cloneDeep(this.refArr);
        const findIndex = _.findIndex(this.refArr, (v: any) => { return v.title == title });
        if (findIndex > -1) {
            _.remove(refArr, (v: any) => { return v.title == title });
            this.refArr = refArr;
            const conf: any = { reference: JSON.stringify(this.refArr) };
            this.$emit("submit-conf", conf);
        }
    }

    updateDomainConf(conf: any) {
        this.$emit("submit-conf", conf);
    }

    @Watch("reference")
    onReferenceChange() {
        this.refArr = this.reference;
    }
}
</script>
<style lang="scss" scoped>
.head-div {
  border-bottom: 1px solid #E4E7ED;
  display: flex;
  padding-bottom: 10px;
}
.user-item-div {
  margin: 10px;
  margin-right: 2px;
  display: flex
}
.owner-btn {
  line-height: 30px;
  color: #CACBCF;
}
.no-data-div {
  line-height: 60px;
  color: #CACBCF;
  padding-left: 10px;
}
.dataSet-label {
  font-weight: 700;
  font-size: 16px;
  color: #999999;
  padding: 0 10px;
  height: 30px;
  line-height: 30px;
}
.nav-item-icon {
  padding-left: 10px;
  display: inline-flex;
  align-items: center;
  height: 33px;
  line-height: 33px;
}
[class^="zeta-icon-"],
[class*=" zeta-icon-"] {
  font-family: "zeta-font", 'ArialMT', 'Arial' !important;
  color: #E4E7ED;
  font-size: 14px;
}
.zeta-icon-add:before {
  padding-right: 5px;
}
</style>

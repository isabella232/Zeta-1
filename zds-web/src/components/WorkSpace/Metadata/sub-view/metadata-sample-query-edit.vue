<template>
    <div class="sample-query-input">
        <el-form label-position="left" :model="form" :rules="rule" ref="form">
            <el-form-item prop="title">
                <el-input placeholder="Title" v-model="form.title"></el-input>
            </el-form-item>
            <el-form-item class="platform-item" label="Platform" prop="platform">
                <el-checkbox-group v-model="form.platform" size="small" @change="selectPlatform">
                    <el-checkbox-button class="platform-css" label="All Platform" border>All Platform</el-checkbox-button>
                    <el-checkbox-button v-for="item in allPlatform" :key="item.toLowerCase()" :label="item.toLowerCase()" class="platform-css" border>{{ item.replace("NuMozart", "Mozart") }}</el-checkbox-button>
                </el-checkbox-group>
            </el-form-item>
            <el-form-item prop="query">
                <SqlEditor class="editor" :readOnly="false" :value="form.query_text" @input="inputSql"></SqlEditor>
            </el-form-item>
            <el-form-item class="btn-div">
                <el-button class="submit-btn" type="primary" @click="submit" v-if="parent != 'register'">Submit</el-button>
                <el-button class="cancel-btn" type="default" plain @click="cancel" v-if="parent != 'register'">Cancel</el-button>
                <el-button class="add-btn" type="default" plain @click="add" v-if="parent == 'register'">{{ isNew ? 'Add' : 'Submit' }}</el-button>
                <el-button class="cancel-btn" type="default" plain @click="cancelRegister" v-if="parent == 'register' && !isNew">Cancel</el-button>
            </el-form-item>
            <div v-if="isNew && parent != 'register'" class="separator-line"></div>
        </el-form>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import conf from "./../metadata-config";
import Util from '@/services/Util.service';
import SqlEditor from "@/components/common/Visualization/CodeDisplay.vue";
import moment from 'moment';
import uuid from "uuid/v4";
@Component({
  components: {
    SqlEditor
  }
})
export default class MetadataSampleQueryEdit extends Vue {
    @Prop() data: any;
    @Prop() isNew: boolean;
    @Prop() allTitle: any;
    @Prop() parent: any;
    @Prop() allPlatform: any;

    cmOptions: any = {
        readOnly: false,
        lineNumbers: false
    }

    form: {
        title: string;
        platform: any;
        query_text: string;
    };

    rule: {
        title: Array<any>;
        platform: Array<any>;
        query_text: Array<any>;
    };

    constructor() {
        super();
        this.form = {
            platform: this.isNew ? (this.data.platform || []) : (!_.isEmpty(this.data.platforms) ? this.data.platforms.split(",") : []),
            title: this.data.title || "",
            query_text: this.data.query_text || ""
        };

        this.rule = {
            title: [
                {
                    required: true,
                    message: "Please input title",
                    trigger: "blur"
                },
                {
                    validator: (r: any, f: string, cb: Function) => {
                        if (this.isNew && _.indexOf(this.allTitle, this.form.title) > -1)
                        cb(
                            new Error(
                            "Duplicate title. Please rewrite new title"
                            )
                        );
                        else cb();
                    }
                }
            ],
            platform: [
                {
                    required: true,
                    message: "Please select platform",
                    trigger: "change"
                }
            ],
            query_text: [
                {
                    required: true,
                    message: "Please input query text",
                    trigger: "blur"
                }
            ]
        }
    }

    selectPlatform() {
        if (!_.isEmpty(this.form.platform)) {
            if (_.last(this.form.platform) == "All Platform") {
                this.form.platform = ["All Platform"];
            }else {
                let filterArr: any = _.cloneDeep(this.form.platform);
                _.remove(filterArr, (v: string) => {return v == "All Platform"});
                this.form.platform = filterArr;
            }
        }
    }
    
    clearEdit() {
        this.form = {
            platform: [],
            title: "",
            query_text: ""
        };
    }

    cancel() {
        this.$emit("operate", this.isNew ? undefined : this.data, false);
        this.clearEdit();
    }

    submit() {
        let valid: boolean = false;
        (this.$refs["form"] as any).validate((valid_: boolean) => (valid = valid_));
            if (!valid) {
            return;
        }
        let platforms: any = "";
        if (this.form.platform.length == 1 && this.form.platform[0] == "All Platform") {
            platforms = _.toLower(_.join(this.allPlatform, ","));
        }else {
            platforms = _.join(this.form.platform, ",");
        }
        const params: any = {
            sample_query_id: this.data.sample_query_id,
            title: this.form.title,
            platforms: platforms,
            query_text: this.form.query_text
        }

        this.$emit("operate", params, this.isNew, true);
        //this.$emit("submit-edit", params);
    }

    add() {
        let valid: boolean = false;
        (this.$refs["form"] as any).validate((valid_: boolean) => (valid = valid_));
        if (!valid) {
            return;
        }
        let platforms: any = "";
        if (this.form.platform.length == 1 && this.form.platform[0] == "All Platform") {
            platforms =_.toLower(_.join(this.allPlatform, ","));
        }else {
            platforms = _.join(this.form.platform, ",");
        }
        const params: any = {
            title: this.form.title,
            platforms: platforms,
            query_text: this.form.query_text,
            upd_user: Util.getNt(),
            upd_date: moment().utcOffset('-07:00').format('YYYY-MM-DD HH:mm:ss'),
            id: this.data.id ? this.data.id : uuid()
        }
        this.$emit("operate", params);
        this.form.title = "";
        this.form.query_text = "";
        this.form.platform = [];
    }

    cancelRegister() {
        this.$emit("cancel", this.data);
    }

    inputSql(value: any) {
        this.form.query_text = value;
    }
}
</script>

<style lang="scss" scoped>
.sample-query-input {
    padding: 10px 0px;
    .platform-item {
        /deep/ .el-form-item__content {
            margin-bottom: 10px;
        }
    }
    .el-checkbox-button {
        /deep/ .el-checkbox-button__inner {
            height: 30px;
            background: inherit;
            background-color: rgba(255, 255, 255, 1);
            border: 1px solid rgba(202, 203, 207, 1);
            border-radius: 4px !important;
            box-shadow: none;
            font-size: 14px;
            color: #CACBCF;
            margin-right: 10px;
            line-height: 10px;
        }
    }
    /deep/ .el-checkbox-button__inner:hover {
        border: 1px solid #4D8CCA;
        color: #4D8CCA;
    }
    .platform-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid #4D8CCA;
        color: #4D8CCA;
    }
    .all-platform-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid #333;
        color: #333;
    }
    .el-checkbox-button.all-platform-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid #333;
        color: #333;
    }

    .apollo_rno-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(91, 94, 183, 1);
        color: #5B5EB7;
    }
    .el-checkbox-button.apollo_rno-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(91, 94, 183, 1);
        color: #5B5EB7;
    }

    .apollo-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(160, 67, 67, 1);
        color: #A04343;
    }
    .el-checkbox-button.apollo-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(160, 67, 67, 1);
        color: #A04343;
    }

    .ares-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(160, 111, 67, 1);
        color: rgba(160, 111, 67, 0.6);
    }
    .el-checkbox-button.ares-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(160, 111, 67, 1);
        color: rgba(160, 111, 67, 0.6);
    }

    .hercules-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(67, 117, 160, 1);
        color: #4375A0;
    }
    .el-checkbox-button.hercules-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(67, 117, 160, 1);
        color: #4375A0;
    }
    
    .hopper-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(139, 173, 156, 1);
        color: #608D75;
    }
    .el-checkbox-button.hopper-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(139, 173, 156, 1);
        color: #608D75;
    }

    .mozart-css:hover /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(154, 97, 151, 1);
        color: #9A6197;
    }
    .el-checkbox-button.mozart-css.is-checked /deep/.el-checkbox-button__inner {
        border: 1px solid rgba(154, 97, 151, 1);
        color: #9A6197;
    }

    .ql-editor {
        padding: 0px;
        margin-bottom: 10px;
    }
    
    .add-btn {
        float: right;
    }
    .separator-line {
        width: 100%;
        height: 1px;
        border-bottom: 1px solid #ccc;
    }
    .editor {
        height: 200px;
        border: 1px solid #dddddd;
        line-height: normal;
        /deep/ .CodeMirror-gutters {
            border: 0px !important;
        }
    }
}
.el-form {
    /deep/ .el-form-item__error {
        white-space: normal;
    }
}

</style>

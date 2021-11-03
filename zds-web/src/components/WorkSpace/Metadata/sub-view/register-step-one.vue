<template>
    <div class="step-one-content">
        <div class="title">
            <span>Register Metadata</span>
        </div>
		<div class="owner-content">
			<el-form label-position="left" :model="table_form" :rules="table_rule" ref="table_form" label-width="150px">
				<el-form-item prop="database" label="Database" class="is-required">
					<!--el-input v-model="table_form.database" style="width: 420px;" @input="initVdmTable()"></el-input-->
					<el-autocomplete class="inline-input" v-model="table_form.database" style="width: 420px;" :fetch-suggestions="vdmDbSearchHandler" :debounce="debounce" @select="handleVdmDbSelect" @input="initVdmTable"></el-autocomplete>
				</el-form-item>
				<el-form-item prop="table_name" label="Table Name" class="is-required">
					<!--el-input v-model="table_form.table_name" style="width: 420px;" @input="initVdmTable()"></el-input-->
					<el-autocomplete class="inline-input" v-model="table_form.table_name" style="width: 420px;" :fetch-suggestions="vdmTableSearchHandler" :debounce="debounce" @select="handleVdmTableSelect" @input="initVdmTable"></el-autocomplete>
					<i class="el-icon-loading" v-if="verify"/>
					<p class="el-loading-text" v-if="verify">Verifying...</p>
					<el-tag :type="type" v-if="validateFlg == 'already'"><span>{{message}}</span></el-tag>
					<i class="el-icon-check" v-if="validateFlg == 'success'"/>
					<el-tag :type="type" v-if="validateFlg == 'danger'"><i class="zeta-icon-error" style="color: #f56C6C;"/><span>{{message}}</span></el-tag>
				</el-form-item>
				<!--el-button type="default" plain class="validate-btn" @click="validate(table_form)">Validate</el-button-->
				<el-form-item prop="platform" label="Platform">
					<el-checkbox-group v-model="table_form.platform" size="small" @change="initVdmTable()">
						<!--el-checkbox-button v-for="item in allPlatform" :key="item" :label="item.toLowerCase()" border disabled>{{ item }}</el-checkbox-button-->
						<i v-for="item in allPlatform" :key="item" :class="item.toLowerCase()" class="platform-icon"/>
					</el-checkbox-group>
				</el-form-item>
			</el-form>
			<div class="separator-line"></div>
			<el-form label-position="left" :model="owner_form" :rules="owner_rule" ref="owner_form" label-width="150px">
				<!--el-form-item prop="ownerDisplay" label="Owner" class="is-required">
					<el-autocomplete class="inline-input" v-model="owner_form.ownerDisplay" :fetch-suggestions="userSearchHandler" :trigger-on-focus="false" :debounce="debounce" @select="handleSelect" @input="handleInput"></el-autocomplete>
				</el-form-item-->
				<el-form-item prop="ownerList" label="Owner" class="is-required">
					<el-tag
						style="margin-left: 0px; margin-right: 10px;"
						:key="tag.nt"
						v-for="tag in owner_form.ownerList"
						closable
						:disable-transitions="false"
						@close="handleOwnerClose(tag)">
						{{tag.name}}
					</el-tag>
					<el-autocomplete
						v-if="inputOwnerVisible"
						ref="saveOwnerInput"
						v-model="newOwnerName"
						class="inline-input"
						:fetch-suggestions="userSearchHandler"
						:trigger-on-focus="false"
						:debounce="debounce"
						@select="handleSelect"
					/>
					<el-button v-else class="button-new-tag" @click="showOwnerInput">+ New Owner</el-button>
				</el-form-item>
				<!--el-form-item prop="team" label="Team" class="is-required">
					<el-input placeholder="" v-model="owner_form.team" style="width: 420px"/>
				</el-form-item-->
				<el-form-item prop="description" label="Description" class="is-required">
					<el-input type="textarea" :rows="2" v-model="owner_form.description" style="width: 600px; padding-top: 4px;"></el-input>
				</el-form-item>
			</el-form>
			<div class="separator-line"></div>
			<el-form label-position="left" :model="detail_form" ref="detail_form" label-width="150px">
				<el-form-item prop="dl" label="DL" class="is-optional">
					<el-input placeholder="example: DL-eBay-ALL@ebay.com" v-model="detail_form.dl" style="width: 420px"/>
				</el-form-item>
				<el-form-item prop="github" label="Github" class="is-optional">
					<el-input v-model="detail_form.github" style="width: 420px;"></el-input>
				</el-form-item>
				<el-form-item prop="script" label="Script" class="is-optional">
					<el-input type="textarea" :rows="3" v-model="detail_form.script" style="width: 600px; padding-top: 4px;"></el-input>
				</el-form-item>
			</el-form>
		</div>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Inject } from 'vue-property-decorator';
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import UserInfoRemoteService from "@/services/remote/UserInfo";
import Util from "@/services/Util.service";
import conf from "./../metadata-config";
import _ from 'lodash';
@Component({
	components: {
		
	}
})

export default class RegisterStepOne extends Vue {
	@Inject('doeRemoteService')
	doeRemoteService: DoeRemoteService;

	@Inject('userInfoRemoteService')
  	userInfoRemoteService: UserInfoRemoteService;

	allPlatform: any = this.$store.getters.getAllPlatform || [];
	userArr: Array<any> = [];
	debounce: number = 500;
	validateFlg: string = "";
	message: string = "";
	type: string = "";
	verify: boolean = false;
	database: string = "";
	table: string = "";
	inputOwnerVisible: boolean = false;
	newOwnerName: string = "";
	owner_form: {
		ownerDisplay: string;
		ownerList: Array<any>;
		owner: string;
		ownerNt: string;
		team: string;
		description: string;
	};
	table_form: {
		platform: Array<any>;
		database: string;
		table_name: string;
	};
	detail_form: {
		dl: string;
		github: string;
		script: string;
	};

    owner_rule: {
		ownerList: Array<any>;
		team: Array<any>;
		description: Array<any>;
	};
	table_rule: {
		database: Array<any>;
		table_name: Array<any>;
	};

    constructor() {
		super();
		const store: any =  this.$store.getters.getRegisterStepOne || {};
		this.userArr = store.userArr || [];
        this.owner_form = {
			ownerDisplay: store.ownerDisplay || "",
			ownerList: store.ownerList || [],
			owner: store.owner || "",
			ownerNt: store.ownerNt || "",
			team: store.team || "",
			description: store.description || ""
		};
		
		this.table_form = {
			platform: store.platform || [],
			database: store.database || "",
			table_name: store.table_name || "",
		};
		
		this.detail_form = {
			dl: store.dl || "",
			github: store.github || "",
			script: store.script || ""
		};
		
		this.validateFlg = store.validateFlg || "";
		this.message = store.message || "";
		this.type = store.type || "";

        this.owner_rule = {
			ownerList: [
				{
					type: 'array',
					required: true,
					message: "Owner cannot be blank!",
					trigger: "blur"
				}
			],
			team: [
				{}
			],
			description: [
                {}
            ]
		};

		this.table_rule = {
            database: [
                {}
            ],
            table_name: [
                {}
            ]
		};
	}

	mounted() {
		if (_.isEmpty(this.owner_form.ownerNt)) this.fullNameByNt(Util.getNt());
		if (_.isEmpty(this.table_form.database) || _.isEmpty(this.table_form.table_name)) {
			this.$store.dispatch("setAllPlatform", []);
			this.allPlatform = [];
		}
	}

	fullNameByNt(nt: string) {
		this.userInfoRemoteService.getBaseInfo(nt).then((res: any) => {
			let userInfo = res.data.response.docs[0];
			this.owner_form.owner =
			res.data.response.docs[0].last_name +
				"," +
				res.data.response.docs[0].first_name;
			this.owner_form.ownerNt = nt;
			this.owner_form.ownerDisplay = this.owner_form.owner + "(" + nt + ")";
			this.owner_form.ownerList.push({nt: nt, name: res.data.response.docs[0].last_name + "," + res.data.response.docs[0].first_name});
		});
	}

	userSearchHandler(queryStr: string, cb: any) {
		this.owner_form.owner = "";
		this.owner_form.ownerNt = "";
        this.doeRemoteService.getUser(queryStr).then(res => {
			const opntions: Array<any> = [];
            if (res && res.data && res.data.response && res.data.response.docs) {
				_.forEach(res.data.response.docs, (v: any) => {
					const option: any = {
						nt: v.nt,
						value: (v.last_name ? v.last_name : "") + 
							   (v.last_name && v.preferred_name ? "," : "") + 
							   (v.preferred_name ? v.preferred_name : "") + 
							   "(" + v.nt + ")",
						name: (v.last_name ? v.last_name : "") + 
							  (v.last_name && v.preferred_name ? "," : "") + 
							  (v.preferred_name ? v.preferred_name : "")
					}

					opntions.push(option);
				})
			}

			this.userArr = opntions;
			cb(this.userArr);
        }).catch(err => {
			console.error("user search failed: " + JSON.stringify(err));
			cb([]);
		});
	}

	handleInput() {
		this.owner_form.owner = "";
		this.owner_form.ownerNt = "";
	}
	
	handleSelect(item: any) {
		this.owner_form.owner = item.name;
		this.owner_form.ownerNt = item.nt;
		this.owner_form.ownerList.push({nt: item.nt, name: item.name});
		this.newOwnerName = '';
		this.inputOwnerVisible = false;
	}

	handleOwnerClose(tag: any) {
		this.owner_form.ownerList.splice(this.owner_form.ownerList.indexOf(tag), 1);
	}

	showOwnerInput() {
		this.inputOwnerVisible = true;
		this.$nextTick(() => {
			const saveOwnerInput: any = this.$refs.saveOwnerInput;
			if (saveOwnerInput) saveOwnerInput.$refs.input.focus();
		});
	}

	vdmDbSearchHandler(queryStr: string, cb: any) {
		const params: any = {
			db: queryStr
		}
		
        this.doeRemoteService.getVdmDB(params).then(res => {
			let opntions: Array<any> = [];
            if (res && res.data && res.data.data && res.data.data.value) {
				_.forEach(res.data.data.value, (v: any) => {
					opntions.push({value: v.db_name});
				})
			}
			cb(opntions);
        }).catch(err => {
			console.error("user search failed: " + JSON.stringify(err));
			cb([]);
		});
	}

	handleVdmDbSelect(item: any) {
		this.table_form.database = item.value;
		if (!_.isEmpty(this.table_form.database) && !_.isEmpty(this.table_form.table_name)) this.validate(this.table_form);
	}
	
	vdmTableSearchHandler(queryStr: string, cb: any) {
		const params: any = {
			table: queryStr,
			db: this.table_form.database
		}
		
        this.doeRemoteService.getVdmTable(params).then(res => {
			let opntions: Array<any> = [];
            if (res && res.data && res.data.data && res.data.data.value) {
				_.forEach(res.data.data.value, (v: any) => {
					opntions.push({value: v.table_name});
				})
			}
			cb(opntions);
        }).catch(err => {
			console.error("user search failed: " + JSON.stringify(err));
			cb([]);
		});
	}

	handleVdmTableSelect(item: any) {
		this.table_form.table_name = item.value;
		if (!_.isEmpty(this.table_form.database) && !_.isEmpty(this.table_form.table_name)) this.validate(this.table_form);
	}

	initVdmTable() {
		this.validateFlg = "";
		this.message = "";
		this.type = "";
		this.allPlatform = [];
		this.owner_form.team = "";
		this.owner_form.description = "";
		this.detail_form.dl = "";
		this.detail_form.github = "";
		this.detail_form.script = "";
		this.$store.dispatch("setAllPlatform", []);
		this.$store.dispatch("setUpdateColumns", []);
		this.$store.dispatch("setRegisterStepThree", []);
	}

	validate(data: any) {
		let valid: boolean = false;
		this.table_rule.database.pop();
		this.table_rule.table_name.pop();
		this.table_rule.database.push({
			required: true,
			message: "Database cannot be blank!",
			trigger: "blur"
		});
		this.table_rule.table_name.push({
			required: true,
			message: "Table name cannot be blank!",
			trigger: "blur"
		});
		(this.$refs["table_form"] as any).validate((valid_: boolean) => (valid = valid_));
		this.table_rule.database.pop();
		this.table_rule.table_name.pop();
		this.table_rule.database.push({});
		this.table_rule.table_name.push({});
        if (!valid) {
            console.debug("Invalid submit");
            return;
        }
        const params: any = {
            db: data.database,
            table: data.table_name
		}
		this.verify = true;
		const confPlatform = conf.allPlatformLowercase;
        this.doeRemoteService.checkVDM(params).then(res => {
            this.validateFlg = "";
			this.message = "";
			this.type = "";
            if (res && res.data && res.data.data && res.data.data.value) {
                const rtVal: any = res.data.data.value;
                const findSt_1: any = _.find(res.data.data.value, v => { return v.status == 1 });
                const findSt_2: any = _.find(res.data.data.value, v => { return v.status == 2 });
                if (findSt_1) {
					this.validateFlg = "already";
					this.message = findSt_1.status_info;
					this.type = "";
                }else if (findSt_2) {
					const platform = _.transform(findSt_2.status_info && findSt_2.status_info != "null" ? findSt_2.status_info.split(",") : [], (rs: any, v: any) => { rs.push(_.toLower(v)) }, []);
					this.allPlatform = _.transform(_.intersection(confPlatform, platform), (rs: any, v: any) => { rs.push(_.upperFirst(_.toLower(v)).replace("mozart", "Mozart")) }, []);
					this.table_form.platform = platform;
					this.$store.dispatch("setAllPlatform", this.allPlatform);
					if (this.allPlatform && !_.isEmpty(this.allPlatform) && _.isArray(this.allPlatform)) {
						this.validateFlg = "success";
						this.type = "success";
					}else {
						this.validateFlg = "danger";
						this.message = "Please select table form drop-down list.";
						this.type = "danger";
					}
                }else {
					this.validateFlg = "danger";
					this.message = "Failed";
					this.type = "danger";
				}
            }else {
				this.validateFlg = "danger";
				this.message = "Failed";
				this.type = "danger";
			}
			this.verify = false;
        }).catch(err => {
            console.error("Call Api:checkVDM failed: " + JSON.stringify(err));
			this.validateFlg = "danger";
			this.message = "Failed";
			this.type = "danger";
			this.verify = false;
		})
	}

	record() {
		const data: any = {
			//ownerDisplay: this.owner_form.ownerDisplay,
			owner: this.owner_form.owner,
			ownerNt: this.owner_form.ownerNt,
			team: this.owner_form.team,
			description: this.owner_form.description,
			platform: this.table_form.platform,
			database: this.table_form.database,
			table_name: this.table_form.table_name,
			dl: this.detail_form.dl,
			github: this.detail_form.github,
			script: this.detail_form.script,
			validateFlg: this.validateFlg,
			message: this.message,
			type: this.type,
			userArr: this.userArr,
			ownerList: this.owner_form.ownerList
		}

		this.$store.dispatch("setRegisterStepOne", data);
	}

	doNext() {
		const valid: boolean = this.check();
		if (valid) {
            console.debug("Invalid submit");
            return;
		}
		if (_.isEmpty(this.allPlatform) || !_.isArray(this.allPlatform)) {
			this.validateFlg = "danger";
			this.message = "Please select table form drop-down list.";
			this.type = "danger";
			return;
		}
		if (this.validateFlg == 'danger') {
			this.$message.info("Please select table form drop-down list.");
			return;
		}else if (this.validateFlg == 'already') {
			this.$message.info("Already registered");
			return;
		}else if (this.validateFlg == '') {
			this.$message.info("Please select table form drop-down list.");
			return;
		}
		this.record();
		this.$emit("ready-do-next");
	}

	check(): boolean {
		this.table_rule.database.pop();
		this.table_rule.table_name.pop();
		this.table_rule.database.push({
			required: true,
			message: "Database cannot be blank!",
			trigger: "blur"
		});
		this.table_rule.table_name.push({
			required: true,
			message: "Table name  cannot be blank!",
			trigger: "blur"
		});

		// this.owner_rule.ownerDisplay.pop();
		//this.owner_rule.ownerList.pop();
		this.owner_rule.team.pop();
		this.owner_rule.description.pop();
		// this.owner_rule.ownerDisplay.push({
		// 	required: true,
		// 	message: "Owner cannot be blank!",
		// 	trigger: "blur"
		// });
		// this.owner_rule.ownerList.push({
		// 	type: 'array',
		// 	required: true,
		// 	message: "Owner cannot be blank!",
		// 	trigger: "blur"
		// });
		this.owner_rule.team.push({
			required: false,
			message: "Team cannot be blank!",
			trigger: "blur"
		});
		this.owner_rule.description.push({
			required: true,
			message: "Description cannot be blank!",
			trigger: "blur"
		});
		
		// if (this.owner_rule.ownerDisplay.length < 2) {
		// 	const rule: any = {
		// 		validator: (r: any, f: string, cb: Function) => {
		// 			if (this.owner_form.owner == '' && this.owner_form.ownerNt == '')
		// 			cb(
		// 				new Error(
		// 				"Invalid input"
		// 				)
		// 			);
		// 			else cb();
		// 		}
		// 	};
		// 	this.owner_rule.ownerDisplay.push(rule);
		// }
		let owner_valid: boolean = false;
		let table_valid: boolean = false;
		let detail_valid: boolean = false;
		(this.$refs["owner_form"] as any).validate((valid_: boolean) => (owner_valid = valid_));
		(this.$refs["table_form"] as any).validate((valid_: boolean) => (table_valid = valid_));
		(this.$refs["detail_form"] as any).validate((valid_: boolean) => (detail_valid = valid_));
		// if (this.owner_rule.ownerDisplay.length > 1) {
		// 	this.owner_rule.ownerDisplay.pop();
		// }
		// this.owner_rule.ownerDisplay.pop();
		this.owner_rule.team.pop();
		this.owner_rule.description.pop();
		//this.owner_rule.ownerDisplay.push({});
		this.owner_rule.team.push({});
		this.owner_rule.description.push({});

		this.table_rule.database.pop();
		this.table_rule.table_name.pop();
		this.table_rule.database.push({});
		this.table_rule.table_name.push({});
		return !owner_valid || !table_valid || !detail_valid;
	}

	getVDMColumns(params: any) {
		this.doeRemoteService.getVDMColumns(params).then(res => {
			const infos: Array<any> = this.$store.getters.getRegisterStepTwo || [];
			if (res && res.data && res.data.data && res.data.data.value) {
				const all: any = res.data.data.value;
				const pick: any = _.partition(all, (sv: any) => { return _.toLower(sv.pltfrm_name) == _.toLower(params.platform) });
				const info: any = {
					platform: params.platform,
					db_name: this.table_form.database,
					table_name:  this.table_form.table_name,
					column: pick ? pick[0] : []
				}
				infos.push(info);
			}
			this.$store.dispatch("setRegisterStepTwo", infos);
		}).catch(err => {
			console.error("Call Api:getVDMColumns failed: " + JSON.stringify(err));
			this.$store.dispatch("setRegisterStepTwo", []);
		});
	}

	getSmartQuery(tables: any) {
		const params: any = { tables: tables, num: 3, isOr: true };
		this.$store.dispatch("setSmartQueryArr", []);
        this.doeRemoteService.getSmartQuery(params).then(res => {
            if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
                this.$store.dispatch("setSmartQueryArr", res.data.data.value);
            }
        }).catch(err => {
            console.error("getSmartQuery: " + JSON.stringify(err));

        });
	}

	getSearchTables(tables: any) {
		const params: any = { tables: tables, num: 10 };
		this.$store.dispatch("setSearchTablesArr", []);
        this.doeRemoteService.getSearchTables(params).then(res => {
            if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
                this.$store.dispatch("setSearchTablesArr", res.data.data.value);
            }
        }).catch(err => {
            console.error("getSearchTables: " + JSON.stringify(err));
        });
    }

	@Watch("validateFlg")
	onValidate(val: any) {
		if (this.validateFlg == 'success') {
			_.forEach(this.table_form.platform, v => {
				const params: any = {
					db: this.table_form.database,
					table: this.table_form.table_name,
					platform: v
				}
				this.getVDMColumns(params);
				this.getSmartQuery(params.db + "." + params.table);
				this.getSearchTables(params.db + "." + params.table);
			})
		}else {
			this.$store.dispatch("setRegisterStepTwo", []);
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';
.step-one-content {
	box-sizing: border-box;
	font-style: normal;
	height: 100%;
	overflow-y: auto;
	padding: 0 25px;
	width: 100%;
}
.title {
	color: #1E1E1E;
	display: block;
	font-size: 18px;
	font-weight: 700;
	height: 36px;
	line-height: 36px;
	padding: 20px 0;
}
.owner-content {
	height: calc( 100% - 76px );
	.el-form {
		/deep/ .el-form-item__label {
			font-weight: 700;
		}
	}
}
.el-form {
	/deep/ .el-form-item__label {
		font-weight: 700;
    }
    .el-checkbox-button {
        /deep/ .el-checkbox-button__inner { 
            background: inherit;
            background-color: #fff;
            border: 1px solid #569ce1 !important;
            border-radius: 4px !important;
            box-shadow: none;
            color: #569ce1;
            font-size: 14px;
            height: 30px;
            line-height: 10px;
            margin-right: 10px;
            min-width: 90px;
        }
       /deep/ .el-checkbox-button__inner:hover {
            border: 1px solid #4d8cca !important;
            color: #4d8cca;
        }
    }
    .el-checkbox-button.is-checked /deep/.el-checkbox-button__inner {
        background-color: #569ce1;
        color: #fff;
    }
    .el-checkbox-button.is-checked /deep/.el-checkbox-button__inner:hover {
        background-color:#4d8cca !important;
        border: 1px solid #4d8cca !important;
        color: #fff;
    }
	.el-tag {
		font-size: 14px;
		margin-left: 10px;
		[class^="zeta-icon-"],
		[class*=" zeta-icon-"] {
			font-size: 16px !important;
		}
		> span {
			margin-left: 5px;
		} 
	}
	/deep/ .el-form-item__error {
		padding: 0;
	}
	/deep/ .el-form-item.is-optional > .el-form-item__label:after {
		content: '(Optional)';
		font-family: 'Arial-ItalicMT', 'Arial Italic', 'Arial';
		font-weight: 400;
		font-style: italic;
		color: #CACBCF;
	}
}
.el-icon-loading {
	margin-left: 10px;
}
.el-loading-text {
	display: inline-block;
    margin-left: 5px;
    font-size: 14px;
    color: #cacbcf;
}
.el-icon-check {
	display: inline-block;
    margin-left: 10px;
    font-size: 20px;
	font-weight: bold;
    color: #5CB85C;
}
.platform-icon {
	font-size: 14px !important;
}
.separator-line {
  border-bottom: 1px solid #f2f2f2;
  height: 1px;
  margin: 20px 0;
}
</style>

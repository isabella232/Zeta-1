<template>
    <div>
        <div class="head-div">
            <p class="dataSet-label">Recent News</p>
            <el-popover placement="bottom" width="400" trigger="click" v-if="!isSubDomain && isOwner && isEdit">
                <div>
                    <el-autocomplete class="inline-input" style="width: 400px; margin-bottom: 10px;" v-model="newProduct" :fetch-suggestions="productSearchHandler" :trigger-on-focus="false" :debounce="debounce" @select="handleProductSelect" @input="handleProductInput" placeholder="Product Name">
                        <el-button slot="append" @click="addProduct">Add</el-button>
                    </el-autocomplete>
                    <el-table :data="productData" height="200">
                    <el-table-column prop="product_name" label="Product Name"></el-table-column>
                    <el-table-column width="50">
                        <template slot-scope="scope">
                        <el-popover placement="top" width="180" v-model="scope.row.visible">
                            <p>Are you sure to delete this?</p>
                            <div style="text-align: right; margin: 0; margin-top:5px;">
                            <el-button size="mini" type="text" @click="scope.row.visible = false">cancel</el-button>
                            <el-button type="primary" size="mini" @click="deleteProduct(scope.row)">confirm</el-button>
                            </div>
                            <el-button type="text" class="cursor" title="Remove Product" slot="reference" @click="scope.row.visible = true">
                            <i class="el-icon-minus"></i>
                            </el-button>
                        </el-popover>
                        </template>
                    </el-table-column>
                    </el-table>
                </div>
                <el-button type="text" slot="reference" class="owner-btn">
                    <p title="Input eNotify Product Name" class="cursor">Register</p>
                </el-button>
            </el-popover>
        </div>
        <div class="news-div" style="max-height: 325px; overflow-y: auto;">
            <div v-for="(item, index) in newArr" :key="item.new_id" class="user-item-div" style="display: flex;">
            <div class="index-div">
                <span class="index">{{ getIndex(index + 1) }}</span>
            </div>
            <div class="info-div">
                <div>
                <span class="name" @click="jump(item.link)">{{ item.enotify_title }}</span>
                </div>
                <div class="content-footer">
                <span class="date">{{ item.cre_date ? item.cre_date : item.create_time }}</span>
                </div>
            </div>
            </div>
            <div v-if="!hasNew" class="no-data-div">No content yet</div>
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
export default class DatasetsEnotify extends Vue {
    @Inject('doeRemoteService')
    doeRemoteService: DoeRemoteService;

    @Prop() isOwner: any;
    @Prop() domain: any;
    @Prop() subDomain: any;
    @Prop() newArr: any;
    @Prop() isEdit: any;

    debounce: number = 500;
    // search func
    userArr: any = [];

    // enotify func
    productData: any = [];
    productArr: any = [];
    newProduct: string = "";
    newProductId: string = "";

    get isSubDomain() {
        this.productData = [];
        if (!_.isEmpty(this.subDomain)) this.getDomainProduct();
        return _.isEmpty(this.subDomain);
    }

    get hasNew() {
        return _.size(this.newArr) > 0;
    }

    mounted() {

    }

    // enotify func
    getIndex(index: any): string {
        return _.padStart(index, 2, "0");
    }

    jump(url: string) {
        window.open(url);
    }

    getDomainProduct() {
        const params: any = { sub_domain: this.subDomain };
        this.newProductId = "";
        this.newProduct = "";
        this.doeRemoteService.getDomainProduct(params).then((res: any) => {
        if (res && res.data && res.data.data && res.data.data.value) {
            this.productData = res.data.data.value;
        }
        });
    }

    productSearchHandler(queryStr: string, cb: any) {
        const params: any = {domain_name: this.subDomain, product_name: queryStr};
        this.doeRemoteService.getSubDomainProductOption(params).then((res:any) => {
        const opntions: Array<any> = [];
        if (res && res.data && res.data.data && res.data.data.value) {
            _.forEach(res.data.data.value, (v: any) => {
            const option: any = {
                id: v.product_id,
                value: v.product_name
            }

            opntions.push(option);
            })
        }

        this.userArr = opntions;
        cb(this.userArr);
        }).catch((err: any) => {
            console.error("user search failed: " + JSON.stringify(err));
            cb([]);
        });
    }

    handleProductInput() {
		this.newProductId = "";
	}
  
    handleProductSelect(item: any) {
		this.newProductId = item.id;
    }

    addProduct() {
        if (this.newProductId == "") {
        this.$message.error("Please select product");
        return;
        }

        const params: any = { domain_name: this.subDomain, product_id: this.newProductId };
        this.doeRemoteService.insertDomainProduct(params).then((res: any) => {
        if (res && res.status == 200) {
            this.getDomainProduct();
            this.$emit('refresh-enotify', { sub_domain: this.subDomain });
        }
        });
    }

    deleteProduct(row: any) {
        const params: any = { domain_name: this.subDomain, product_id: row.product_id };
        this.doeRemoteService.delDomainProduct(params).then((res: any) => {
            if (res && res.status == 200) {
                this.getDomainProduct();
                this.$emit('refresh-enotify', { sub_domain: this.subDomain });
            }
        });
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
}
.owner-btn {
  float: right;
  line-height: 30px;
}
.no-data-div {
  line-height: 60px;
  color: #CACBCF;
  padding-left: 10px;
}
.news-div {
  display: flex;
  flex-direction: column;
  .index-div {
    text-align: center;
    font-size: 28px;
    color: #CACBCF;
    padding-right: 20px;
  }
  .name {
    cursor: pointer;
    font-size: 16px;
    font-weight: 700;
  }
  .name:hover {
    color: #569CE1;
  }
  .content-footer {
    color: #CACBCF;
    font-size: 14px;
    padding-top: 5px;
    .date {
      padding-right: 20px;
    }
  }
}
.dataSet-label {
  font-weight: 700;
  font-size: 16px;
  color: #999999;
  width: 100%;
  padding: 0 10px;
  height: 30px;
  line-height: 30px;
}
.el-table {
  width: 100%;
  .table-desc-label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  /deep/ th, td {
    font-size: 14px;
  }
  /deep/ .top-row {
    background-color: #F9F9FA;
  }
  /deep/ .el-tag {
    margin-right: 10px;
  }
  /deep/ .el-slider__button-wrapper {
    display: none !important;
  }
  /deep/ .el-slider__bar {
    height: 15px !important;
    background-color: #569ce1 !important;
    border-top-right-radius: 3px;
    border-bottom-right-radius: 3px;
  }
  /deep/ .el-slider__runway {
    height: 15px !important;
    margin: 0px 0px !important;
  }
}
.cursor {
  cursor: pointer;
}
</style>

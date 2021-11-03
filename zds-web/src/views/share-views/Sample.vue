<template>

	<div class="sample">
		<el-menu :default-active="index" class="el-menu-demo" mode="horizontal" @select="(k) => index = k">
			<el-menu-item index="1">Basic</el-menu-item>
			<el-submenu index="2">
				<template slot="title">Components</template>
				<el-menu-item index="2-1">Table</el-menu-item>
				<el-menu-item index="2-2">Custom Components</el-menu-item>
			</el-submenu>
		</el-menu>
		<template v-if="index == '1'">
			<section>
				<h2>UI library: <a href="https://element.eleme.io">Element.io</a></h2>
				<h4>internal constant <a href="https://github.corp.ebay.com/DSS-CCOE/zeta-dev-suite/blob/dev/zds-web/src/styles/global.scss">global.scss</a></h4>
				<h4>internal custom css <a href="https://github.corp.ebay.com/DSS-CCOE/zeta-dev-suite/blob/dev/zds-web/src/styles/custom.scss">custom.scss</a></h4>
			</section>
			<section>
				<h2>
					font
				</h2>
				<div class="row">
					<span>Default:'ArialMT', 'Arial</span>
				</div>
				<div class="row">
					<span class="font-bord">Bord: 'Helvetica-Bold', 'Helvetica Bold', 'Helvetica'</span>
				</div>
			</section>
			<section class="color">
				<h2>Colors</h2>
				<div class="row">
					<span class="color-global">theme:#569ce1</span>
					<span class="color-global-hightlight">hightlight:#4d8cca</span>
					<span class="color-global-disable">disable:#CACBCF</span>
				</div>
				<br/>
				<div class="row">
					<span class="color-lable-1">lable-1:#F3F4F6</span>
					<span class="color-lable-2">lable-2:#F9F9FA</span>
				</div>
			</section>
			<section>
				<h2>Buttons</h2>
				<div class="row">
					Size:
					<el-button type="primary" size="mini">mini</el-button>
					<el-button type="primary" size="small">small</el-button>
					<el-button type="primary">Normal</el-button>
					<el-button type="primary" size="medium">medium</el-button>
					<el-button type="primary" size="large">large</el-button>
					
				</div>
				<div class="row">
					<el-button>Default</el-button>
					<el-button type="primary">Primary</el-button>
					<el-button type="success">Success</el-button>
					<el-button type="info">Info</el-button>
					<el-button type="warning">Warning</el-button>
					<el-button type="danger">Danger</el-button>
					<el-button :disabled="true">Disable</el-button>
					<el-button type="text">text</el-button>
				</div>
				<div class="row">
					<el-button plain>Default</el-button>
					<el-button plain type="primary">Primary</el-button>
					<el-button plain type="success">Success</el-button>
					<el-button plain type="info">Info</el-button>
					<el-button plain type="warning">Warning</el-button>
					<el-button plain type="danger">Danger</el-button>
					<el-button plain :disabled="true">Default</el-button>
					<el-button plain type="text">text</el-button>
				</div>
			</section>
			<section>
				<h2>Input & Select</h2>
				<div>line-height:30px; height:30px;</div>
				<div class="row">
					<el-input v-model="input"/>
				</div>

				<div class="row">
					<el-input v-model="input" readonly ref="input" class="link">
						<el-button slot="append" >Copy link</el-button>
					</el-input>
				</div>

				<div class="row">
					<el-input type="textarea" v-model="textarea"/>
				</div>
				<div class="row">
					<el-select v-model="select" multiple placeholder="select">
						<el-option
						v-for="(item,$i) in selectOptions"
						:key="$i"
						:label="item"
						:value="item">
						</el-option>
					</el-select>
				</div>
			</section>
		</template>
		<template v-if="index == '2-1'">
			<section>
				<h2>Table(Element)</h2>
				<div class="row">
					<el-table
						:data="tableData"
						style="width: 100%">
						<el-table-column
						sortable
							prop="date"
							label="日期"
							width="180">
						</el-table-column>
						<el-table-column
							prop="name"
							label="姓名"
							width="180">
						</el-table-column>
						<el-table-column
							prop="address"
							label="地址">
						</el-table-column>
					</el-table>
				</div>
			</section>
		</template>
		<template v-if="index == '2-2'">
			<section>
				<h2>Custom Components</h2>
				<section>
					<h3>
						Share Link
					</h3>
					<div>{{link}}</div>
					<div class="row">
						<ShareLink v-model="link"/>
					</div>
				</section>
				<section>
					<h3>
						Dropdown
					</h3>
					<div class="row">
						<ZetaDropdown v-model="dropdown_select" :options="selectOptions" />
					</div>
				</section>
				<section>
					<h3>
						Github Pull/Push Request
					</h3>
					<div class="row">
						<PullRequest :default-link="'https://github.corp.ebay.com/DSS-CCOE/zeta-dev-suite'"/>
					</div>
				</section>

			</section>
		</template>

	</div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

import ShareLink from '@/components/common/share-link';
import ZetaDropdown from '@/components/common/dropdown/dropdown.vue';
import PullRequest from '@/components/Repository/github/pull-request'
@Component({
  components: {
	  ShareLink,
	  ZetaDropdown,
	  PullRequest
  },
})
export default class Sample extends Vue {
	index = '1';
	readonly selectOptions = ['option1','option2','option3',]
	input = 'input'
	textarea = 'line 1 \nline2'
	select = []
	tableData = [{
            date: '2016-05-02',
            name: '1234',
            address: '12345'
          }, {
            date: '2016-05-04',
            name: '王小虎',
            address: '上海市普陀区金沙江路 1517 弄'
          }, {
            date: '2016-05-01',
            name: '王小虎',
            address: '上海市普陀区金沙江路 1519 弄'
          }, {
            date: '2016-05-03',
            name: '王小虎',
            address: '上海市普陀区金沙江路 1516 弄'
		  }];
	link = 'https://zeta.corp.ebay.com'

	dropdown_select= 'option1';
	// dropdown_options = [ 'option1', 'option2','option3']
	
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.sample{
	// height: 100%;
	overflow: auto;
}
section{
	margin: 10px 0;
	padding: 0 15px;
	div.row{
		margin: 5px 0;
	}
}
.font-bord{
	font-family: $font-bord
}
.color{
	span{
		margin: 0 10px;
	}
	.color-global{
	background-color: $zeta-global-color;
	}
	.color-global-hightlight{
		background-color: $zeta-global-color-heighlight;
	}
	.color-global-disable{
		background-color: $zeta-global-color-disable;
	}
	.color-lable-1{
		background-color:$zeta-label-lvl1;
	}
	.color-lable-2{
		background-color:$zeta-label-lvl2;
	}
}

</style>


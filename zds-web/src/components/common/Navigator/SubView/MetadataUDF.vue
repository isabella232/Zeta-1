<template>
  	<div class="udf">
		<div class="udf-title" @click="() => toggle()">
			<i class="toggle-icon" :class="showUDF ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" />
			UDF Browse
		</div>
		 <transition name="el-zoom-in-top">
			<ul v-show="showUDF"  class="udf-list">
				<li v-for="(udf,$i) in UDFList" :key="$i">{{udf}}</li>
			</ul>
		 </transition>
  	</div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component({
  components: {
  },
})
export default class MetadataUDF extends Vue {
	private showUDF:boolean = true;
	get UDFList():string[]{
		return [
			'udf1111','udf2222','udf3333'
		]
	}
	toggle(){
		this.showUDF = !this.showUDF
	}
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.udf{
	.udf-title{
		cursor: pointer;
	}
	ul.udf-list{
		margin-left: 15px;
		list-style-type: none;

		> li {
			padding: 5px 0;
			cursor: pointer;
			border-bottom: 1px solid #CCC;
			&:hover{
				color:$zeta-global-color-heighlight;
			}
		}
	}
}
</style>



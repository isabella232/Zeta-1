<template>
	<div class="home">
		<el-radio-group v-model="$p" @change="platformChangeEventHandler">
			<el-radio v-for="plat in platformOptions" :key="plat" :label="plat">{{plat}}</el-radio>
		</el-radio-group>
		<el-radio-group v-model="$db" @change="databaseChangeEventHandler">
			<el-radio v-for="db in databaseOptions" :key="db" :label="db">{{db || 'default'}}</el-radio>
		</el-radio-group>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit } from 'vue-property-decorator';
import { platform } from '@/types/workspace';

@Component({
  components: {
  },
})
export default class DatabaseSelecter extends Vue {
	@Prop()
	platform:platform;
	get $p(){
		return this.platform
	}
	set $p($val){
		this.platform = $val
	}
	@Prop()
	platformOptions:platform[];

	@Prop()
	database:string;
	get $db(){
		return this.database
	}
	set $db($val){
		this.database = $val
	}
	@Prop()
	databaseOptions:string[];
	platformChangeEventHandler($val:platform){
		this.$emit("onPlatformChange",$val)
	}
	databaseChangeEventHandler($val:string){
		this.$emit("onDatabaseChange",$val)
	}
	@Emit('onDatabaseChange')
	onDatabaseChange(database:string){

	}
	@Emit('onPlatformChange')
	onPlatformChange(platform:platform){

	}
}
</script>


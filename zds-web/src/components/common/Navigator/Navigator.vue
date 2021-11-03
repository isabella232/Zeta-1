<template>
	<div class="nav" ref="nav">
		<div class="nav-list" v-bind:class="{'closed':!showAll}" @mouseover="()=> open()" @mouseout="() => close()">
			<div class="nav-items-top">
				<div class="nav-logo" v-click-metric:NAV_CLICK="{name: 'Logo'}" @click="() => clickHandler(workspaceCfg)">
						<div class="nav-item-icon">
							<img :src="'./img/zeta_image_white.svg'" />
						</div>
						
						<div class="nav-item-name">
							<img :src="'./img/zeta_font_white.svg'" />
						</div>
				</div>
				<ul class="nav-items-list">
					<li  v-click-metric:NAV_CLICK="{name: n.name}" v-for="n in navs" :key="n.id" class="nav-item" v-bind:class="{'active':activeItemName == n.name}" @click="() => clickHandler(n)" :title="n.name">
						<div class="nav-item-icon">
							<i :class="n.iconClass" class=""></i>
						</div>
						
						<div class="nav-item-name">{{n.name}}</div>
					</li>
				</ul>
			</div>
			<div class="nav-items-bottom"> 
				<ul class="nav-items-list">
					<li  v-click-metric:NAV_CLICK="{name: n.name}" v-for="n in tools" :key="n.id" class="nav-item" @click="() => clickHandler(n)" v-bind:class="{'active':activeItemName == n.name}" :title="n.name">
						<div class="nav-item-icon">
							<i :class="n.iconClass" class=""></i>
						</div>
						
						<div class="nav-item-name">{{n.name}}</div>
					</li>
					<!-- avatar -->
					<li  v-click-metric:NAV_CLICK="{name: 'Avatar'}" key="user" class="nav-item" @click="() => clickHandler(user)" v-bind:class="{'active':activeItemName == user.name}" :title="user.name">
						<div class="nav-item-icon">
							<span v-bind:style="{backgroundImage:'url(./img/icons/user_white.png)'}" class="avatar-bg">
								<span v-bind:style="{backgroundImage:'url('+user.iconClass+')'}" class="avatar"></span>
							</span>
						</div>
						
						<div v-show="showAll" class="nav-item-name">{{user.name}}</div>
					</li>
				</ul>
			</div>
		</div>
		<keep-alive>
			<component class="nav-subview" v-show="currentSubView" :is="currentSubView" @navSubViewClose="close(true)"/>
		</keep-alive>
		
	</div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Provide } from 'vue-property-decorator';
import config from '@/config/config';
import { config as NavCfg, toolsConfig, NavItem, NavType, NavPath, NavAction, NavSubView, ZETA_ACTIONS } from './nav.config'
// import { NavItem, NavType, NavPath, NavAction, NavSubView } from './navigator.define';
import Util from '@/services/Util.service';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import MetadataService from "@/services/Metadata.service";
import { EventBus } from '@/components/EventBus'
import _ from 'lodash';
import $ from 'jquery'
import { ZetaExceptionProps } from '@/types/exception';
import ZeppelinApi from '@/services/remote/ZeppelinApi';

let cmpnts:Dict<any> = {};
_.chain(NavCfg).union(toolsConfig).forEach(nav => {
	if(nav.type === NavType.SUB_VIEW){
		
		cmpnts[(nav as NavSubView).componentName] = (nav as NavSubView).component;
	}
}).value()
@Component({
	components: {
		...cmpnts
	},
})
export default class Navigator extends Vue {
	private mouseIn:boolean = false;
	private currentSubView:string = '';
	@Provide('notebookRemoteService')
	notebookRemoteService = new NotebookRemoteService()
	@Provide('zeppelinApi')
	zeppelinApi = new ZeppelinApi();
	@Provide()
	metadataService = new MetadataService()
	@Inject() reload:any;
	get showAll():boolean {
		if(!this.currentSubView){
			return this.mouseIn;
		}
		else{
			return false;
		}
	}
	get user():NavItem{
		let userName = this.$store.getters.user.profile.firstName + " " +this.$store.getters.user.profile.lastName
		return {
			id:"user",
			name:userName,
			type:NavType.PATH,
			iconClass:`//ihub.corp.ebay.com/images/ldap/${Util.getNt()}.jpg`,
			path:"/settings"
		}
	}
	get navs():NavItem[]{
			return NavCfg
	}
	get tools():NavItem[]{
			return toolsConfig
	}
	get activeItemName():string {
		let items:NavItem[] = _.chain(NavCfg).union(toolsConfig).push(this.user).value()
		let activeName:string = '';
		if(this.currentSubView){
			let activeNav = items.find( n => n.type == NavType.SUB_VIEW && this.currentSubView == (<NavSubView>n).componentName )
			activeName = activeNav ? activeNav.name : '';
		}
		else {
			let path = this.$route.path;
			let activeNav = items.find( n => n.type == NavType.PATH && path == (<NavPath>n).path )
			activeName = activeNav ? activeNav.name : '';
		}
		return activeName;
	}
	get $nav(){
		return this.$refs['nav']
	}
	get workspaceCfg(){
		return NavCfg.find( ({id}) =>  id == 'Workspace')
	}
	open(){
		this.mouseIn = true;
	}
	close(all:boolean = false){
		this.mouseIn = false;
		if(all){
			this.currentSubView = '';
		}
	}
	private windowClickHandler = (e:MouseEvent) => {
		if(!this.$nav){
			return 
		}
		if((this.$nav as HTMLElement).contains(e.target as any)){

		}
		else{
			this.close(true);
		}
	}
	mounted(){
		const props:ZetaExceptionProps  = {
			path: 'navigator'
		}
		this.metadataService.metedataRemoteService.props(props);
		this.notebookRemoteService.props(props)
		this.zeppelinApi.props(props);
		window.addEventListener("click",this.windowClickHandler,true)
		EventBus.$on('open-sub-view',(nav:NavSubView) => {
			console.debug('get event open-sub-view',nav)
			this.subViewClick(nav);
		})
	}
	destory(){
		EventBus.$off('open-sub-view');
		window.removeEventListener("click",this.windowClickHandler,true)
	}
	clickHandler(nav:NavItem){
		switch (nav.type){
			case NavType.PATH:
				if((<NavPath>nav).path){
					this.pushTo((<NavPath>nav))
				}
				this.currentSubView = '';
				break;
			case NavType.LINK:
				if((<NavPath>nav).path){
					// Util.unregisterBeforeunload()
					// location.href = (<NavPath>nav).path
					// setTimeout(()=>Util.registerBeforeunload(),30)
					window.open((<NavPath>nav).path)
				}
				this.currentSubView = '';
				break;
			case NavType.ACTION:
				if(!!(<NavAction>nav).action){
					this.handleAction(<NavAction>nav)
				}
				this.currentSubView = '';
				break;
			case NavType.SUB_VIEW:
				if((<NavSubView>nav).component){
					this.subViewClick(<NavSubView>nav)
				}
				break;
			default:
				break;
		}
	}
	pushTo(nav:NavPath){
		if(this.$appName == 'zeta') {
			this.$router.push(nav.path);
			// nav.config.ts: define whether you need to refresh the page each time
			// ex: release page
			if (nav.meta && nav.meta.reload) {
				this.reload();
			}
		}
		else{
			let url = `${location.protocol}//${location.host}/${Util.getPath()}#${nav.path}`
			window.open(url, 'zeta');
		}
	}
	handleAction(nav:NavAction){
		if(this.$appName == 'zeta'){
			(<NavAction>nav).action(EventBus,this.$router,this.$store)
		}
		else{
			let url = `${location.protocol}//${location.host}/${Util.getPath()}#/notebook?internal_action=${nav.zetaActions}`
			window.open(url, 'zeta');
		}
	}
	subViewClick(nav:NavSubView){
		if(this.$appName == 'zeta'){
			if(nav.component){
				if(this.currentSubView == (<NavSubView>nav).componentName){
					this.currentSubView = '';
				}
				else{
					this.currentSubView = (<NavSubView>nav).componentName;
				}
				
			}
		}
		else{
			let url = `${location.protocol}//${location.host}/${Util.getPath()}#/notebook?internal_action=${nav.zetaActions}`
			// location.href = url
			window.open(url, 'zeta');
		}
	}
}
</script>
<style lang="scss" scoped>
$nav-bg-color:#282E35;
$nav-bg-hover-color:#3D4349;
$nav-color:#94BFE1;

$navWidth:220px;
$navIconWidth:70px;
$navItemHeight:50px;
.nav{
	position: fixed;
	overflow: hidden;
	top:0;
	left:0;
	display: flex;
	flex-direction: row;
	height: 100%;
	transition: width .3s ease;
	box-shadow:2px 0px 6px 0px rgba(0, 0, 0,.2);
	.nav-logo{
		cursor: pointer;
		text-align: center;
		height: $navItemHeight;
		display: flex;
		margin: 20px 0 40px 0;
		.nav-item-icon{
			width: $navIconWidth;
			display: flex;
			justify-content: center;
			align-items: center;
			> img{
				width: 24px;
			}		
		}
		.nav-item-name{
			width: $navWidth - $navIconWidth;
			font-size: 20px;
			display: flex;
			justify-content: flex-start;
			align-items: center;
			> img{
				width: 60px;
			}		
		}
	}
	.nav-list{
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		width: $navWidth;
		background-color: $nav-bg-color;
		overflow-y: auto;
		overflow-x: hidden;
		&.closed{
			width: $navIconWidth;
		}
		.nav-items-top,
		.nav-items-bottom{
			min-width: $navWidth;
		}
		.nav-item{
			color: #94BFE1;
			text-align: center;
			height: $navItemHeight;
			display: flex;
			cursor: pointer;
			&:hover{
				background-color: mix(#FFF,$nav-bg-color,5%)
			}
			&.active{
				background-color: mix(#FFF,$nav-bg-color,10%)
			}
			.nav-item-icon{
				width: $navIconWidth;
				display: flex;
				justify-content: center;
				align-items: center;
				[class^="zeta-icon-"],
				[class*=" zeta-icon-"] {
					font-size: 24px;
					color:#94BFE1
				}
				.avatar-bg{
					border-radius: 15px;
					display: block;
					background-position: center;
					width: 30px;
					height: 30px;
					background-size: 30px 30px;
				}
				.avatar{
					border-radius: 15px;
					display: block;
					background-position: center;
					width: 30px;
					height: 30px;
					background-size: 30px 40px;
				}			
			}
			.nav-item-name{
				width: $navWidth - $navIconWidth;
				font-size: 20px;
				display: flex;
				justify-content: flex-start;
				align-items: center;
				font-family: 'ArialMT', 'Arial';
				font-weight: 400;
				font-style: normal;
				font-size: 14px;
			}
		}
	}
	.nav-subview{
		background-color: #F4F7F9; //mix(#F4F7F9,#000,20%);
		opacity: .98;
	}
}
</style>


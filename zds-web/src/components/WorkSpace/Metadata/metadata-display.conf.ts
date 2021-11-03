import _ from 'lodash'
import { MetadataDetail } from './index';
const defaultTDCfg = [
	{
		id: 'view',
		name: 'View',
		formatter:(m:MetadataDetail) =>       
		m.name
				? `${m.system.dbName.toUpperCase()}.${m.name.toUpperCase()}`
				: `default.${m.name.toUpperCase()}`
	},
	{
		id: 'full_name',
		name: 'Table',
		formatter: (m: MetadataDetail) =>
			m.system.dbName
				? `${m.system.dbName.toUpperCase()}.${m.name.toUpperCase()}`
				: `default.${m.name.toUpperCase()}`
	},
	{
		id: 'frequency',
		name: 'Data refresh Frequency',
		formatter: (m: MetadataDetail) => m.frequency
	},
	{
		id: 'touch_file',
		name: 'Touch File',
		formatter: (m: MetadataDetail) => m.touchFile
	},
	{
		id: 'upd_time',
		name: 'Last Update',
		formatter: (m: MetadataDetail) => m.updTime
	},
	{
		id: 'subject_area',
		name: 'SA',
		formatter: (m: MetadataDetail) => m.subjectArea
	},
	{
		id: 'product_owner',
		name: 'Owner',
		formatter: (m: MetadataDetail) => m.productOwner,
		imgUrl: (m: MetadataDetail) =>
			`//ihub.corp.ebay.com/images/ldap/${m.productOwner}.jpg`
	},
	{
		id: 'team_dl',
		name: 'Team DL',
		formatter: (m: MetadataDetail) => m.teamDl
	},
	{
		id: 'access_cnt',
		name: 'Poularity',
		formatter: (m: MetadataDetail) => m.distinctBatchCnt + m.distinctUserCnt
	}
];
const defaultSparkCfg = [
	{
		id: 'view',
		name: 'View',
		formatter:(m:MetadataDetail) =>       
		m.name
				? `${m.system.dbName.toUpperCase()}.${m.name.toUpperCase()}`
				: `default.${m.name.toUpperCase()}`
	},
	{
		id: 'full_name',
		name: 'Table',
		formatter: (m: MetadataDetail) =>
			m.system.dbName
			? `${m.system.dbName.toUpperCase()}.${m.name.toUpperCase()}`
			: `default.${m.name.toUpperCase()}`
	},
	{
		id: 'frequency',
		name: 'Data refresh Frequency',
		formatter: (m: MetadataDetail) => m.frequency
	},
	{
		id: 'hdfs_location',
		name: 'HDFS Folder',
		formatter: (m: MetadataDetail) => m.hdfsLocation
	},
	{
		id: 'touch_file',
		name: 'Touch File',
		formatter: (m: MetadataDetail) => m.touchFile
	},
	{
		id: 'upd_time',
		name: 'Last Update',
		formatter: (m: MetadataDetail) => m.updTime
	},
	{
		id: 'subject_area',
		name: 'SA',
		formatter: (m: MetadataDetail) => m.subjectArea
	},
	{
		id: 'product_owner',
		name: 'Owner',
		formatter: (m: MetadataDetail) => m.productOwner,
		imgUrl: (m: MetadataDetail) =>
			`//ihub.corp.ebay.com/images/ldap/${m.productOwner}.jpg`
	},
	{
		id: 'team_dl',
		name: 'Team DL',
		formatter: (m: MetadataDetail) => m.teamDl
	},
	{
		id: 'access_cnt',
		name: 'Poularity',
		formatter: (m: MetadataDetail) => m.distinctBatchCnt + m.distinctUserCnt
	}
];
const platformMap = ['Mozart',"Vivaldi","Hercules","Apollo","Ares"]
const cfgByPlat:Dict<any> = {};
_.forEach(platformMap, plat => {
    if(plat === 'Mozart' || plat === 'Vivaldi' || plat === 'Hopper' ){
        cfgByPlat[plat] = defaultTDCfg
    }
    else {
        cfgByPlat[plat] = defaultSparkCfg
    }
        
})
const HadoopPlatform = ["Hercules","Apollo","Ares"]
export {cfgByPlat,HadoopPlatform};
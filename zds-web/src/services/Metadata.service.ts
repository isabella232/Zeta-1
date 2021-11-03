import {  Source, IMetadata, rowType, platform } from "@/types/workspace";
import MetadataRemoteService from '@/services/remote/Metadata';
import _ from 'lodash';
import moment from "moment";
export default class MetadataService {
    metedataRemoteService: MetadataRemoteService
    constructor() {
        this.metedataRemoteService = new MetadataRemoteService()
    }
    parseMetadataList(sources:any[]):Source[]{
        return _.chain(sources).map((source:any) => {
            let platformOptions = source.platform.split(",")
            let rowType = source.row_type.split(",")
            return <Source>{
                name:source.name.toUpperCase(),
                platformOptions,
                desc:source.desc || '',
                rowType,
                sort:source.total_score
            }
        }).sortBy(['_source.total_score','asc']).value()
    }
    parseMetadataDetail(res:any) {
        return res.data.value;
    }
    queryMetadataList(queryStr:string){
        queryStr = queryStr.toLowerCase();
        return this.metedataRemoteService.queryMetadataList(queryStr)
		.then(this.parseMetadataList)
    }
    async queryMetadataDetail(meta:IMetadata){
        let metaDetail;
        if(_.includes(meta.rowTypeOptions,'table')){
            metaDetail = await this.metedataRemoteService.queryTableDetail(meta.name).then(this.parseMetadataDetail);
        } else {
            metaDetail = await this.metedataRemoteService.queryViewDetail(meta.name).then(this.parseMetadataDetail);
        }
        console.debug("source data",metaDetail)
        let type:rowType = (meta.rowTypeOptions.indexOf('table') >= 0) ? 'table' : 'view'
        metaDetail = this.mergeDesc(metaDetail);
        const metaDetailByPlat =  _.chain(metaDetail).groupBy('platform').value()
        const mergedData = _.mapValues(metaDetailByPlat,(details,platform) => this.mergeMetadata(details,type))
        console.debug("merged",mergedData);
        
        meta.detailDict = mergedData;
        meta.platformOptions = this.sortPlatforms(mergedData);
        meta.platform = meta.platformOptions[0]
        return meta
    }
    mergeDesc(details:any[]){
        let desc = _.reduce(details,(result,item) =>{
            if(item.table_desc && item.table_desc.length > result.length){
                result = item.table_desc
            }
            return result;
        },'')
        details.forEach(d => d.table_desc = desc)
        return details;
    } 
    mergeMetadata(details:any[],type:rowType){
        let mergedData:Dict<any> = {}
        details = _.sortBy(details,[ (v:any)=> -(v.distinct_batch_cnt + v.distinct_user_cnt)])
        details = details.map(v => {
            if(type === 'table' && v.view && v.view.length > 0){
                v.view = _.chain(v.view).map((view:any) => {
                    if(view.view_name.toUpperCase() === v.table_name.toUpperCase()){
                        return `${view.view_db.toUpperCase()}.${view.view_name.toUpperCase()}`
                    }
                    
                }).pull(undefined).value()
            }

            if(type === 'view' && v.table && v.table.length > 0){
                v.table = v.table.map((table:any) => {
                    return `${table.db_name.toUpperCase() || 'default'}.${table.table_name.toUpperCase()}`
                })
                v.view = [`${v.view_db.toUpperCase() || 'default'}.${v.view_name.toUpperCase()}`]
            }
            return v;
        })
        mergedData = _.reduce(details,(result,item,index) => {
            // first && not null
            if(type === 'table'){
                if(!result.db_name && item.db_name){
                    result.db_name = item.db_name;
                }
                if(!result.table_name && item.table_name){
                    result.table_name = item.table_name;
                }
            }

            if(!result.frequency && item.frequency){
                result.frequency = item.frequency;
            }
            if(!result.hdfs_location && item.hdfs_location){
                result.hdfs_location = item.hdfs_location;
            }
            if(!result.touch_file && item.touch_file){
                result.touch_file = item.touch_file;
            }
            if(!result.subject_area && item.subject_area){
                result.subject_area = item.subject_area;
            }
            if(!result.product_owner && item.product_owner){
                result.product_owner = item.product_owner;
            }
            if(!result.team_dl && item.team_dl){
                result.team_dl = item.team_dl;
            }
            
            // sum
            result.distinct_batch_cnt += item.distinct_batch_cnt || 0;
            result.distinct_user_cnt += item.distinct_user_cnt || 0;

            // check length
            if(result.table_desc.length < item.table_desc.length){
                result.table_desc = item.table_desc
            }
            if(result.column.length < item.column.length){
                result.column = item.column.sort( (c1:any, c2:any) => c1.column_id - c2.column_id)
                 
            }

            // union
            if(type == 'table'){
                result.view = _.union(result.view,item.view || [])
            }
            else if ( type == 'view'){
                if(result.table.length == 0 && item.table.length > 0){
                    result.table = item.table
                }
                result.view = _.union(result.view,item.view || [])
                // result.table = _.union(result.table,item.table || [])
            }

            // check time
            let date = moment(result.upd_time);
            let itemUpdDate = moment(item.upd_time)
            if (date.isBefore(itemUpdDate)){
                result.upd_time = item.upd_time
            }
            return result
        },{
            db_name:"",
            table_name:"",
            view_db:"",
            view_name:"",
            frequency:"",
            hdfs_location:"",
            touch_file:"",
            subject_area:"",
            product_owner:"",
            team_dl:"",
            distinct_batch_cnt:0,
            distinct_user_cnt:0,
            table_desc:"",
            view:[],
            table:[],
            column:[],
            upd_time:"2010-01-01"
        })
        return mergedData
    }
    sortPlatforms(detailDict:Dict<any>):platform[]{
        return _.chain(detailDict).map((metadata,platform) => {
            return {
                platform,
                poularity:metadata.distinct_batch_cnt + metadata.distinct_user_cnt
            }
        }).sortBy([(metadata:{platform:platform,poularity:number}) => - metadata.poularity])
        .map((metadata:any )  =>  {
            return metadata.platform
        } )
        .value()
    }
    updateESTableDesc(queryStr: string, desc: string) {
        queryStr = queryStr.toLowerCase().trim();
        return this.metedataRemoteService.updateESTableDesc(queryStr, desc);
    }
    putESTable(params: any, data: any) {
        return this.metedataRemoteService.putESTable(params, data);
    }
    queryTableauList(queryStr:string){
        queryStr = queryStr.toLowerCase();
        return this.metedataRemoteService.queryTableauList(queryStr)
		.then(this.parseTableauList)
    }
    parseTableauList(sources:any[]): any{
        return _.chain(sources).map((source:any) => {
            return {
                asset_name: source.asset_name,
                prj_name: source.prj_name || '',
                parent_loc: source.parent_loc || '',
                child_loc: source.child_loc || '',
                asset_ref_id: source.asset_ref_id || '',
                snapshot: source.asset_ref_id ? 'https://tableau.corp.ebay.com/vizportal/api/rest/v1/views/' + source.asset_ref_id + '/thumbnail' : '',
                user: source.user || '',
                sort: source.user_cnt || 0,
                image_base64: source.image_base64 || ''
            }
        }).sortBy(['_source.user_cnt','asc']).value()
    }
}
import { platform } from "@/types/workspace";
import _ from "lodash";

interface SystemInfo{
    platform:platform
    dbName:string
}
class MetadataBase{
    id:string
    name:string
    system:SystemInfo
    constructor(){
        this.system = {
            platform : 'Hercules',
            dbName : ''
        };
        this.name = "";
    }
}
interface MetadataSubView {
    revert:Function
    save:Function
}
interface SubView{
    viewDb:string
    viewName:string
    disable?:boolean
}
interface SubTable{
    dbName:string
    tableName:string
    disable?:boolean
}
interface Column{
    columnId:number
    columnName:string
    columnDesc:string
    dataType:string
    [k:string]:any
}
interface ColumnWrapper extends Column{
    dbMap:Dict<string[]>
    dataTypeMap:Dict<Dict<string>>
}

class MetadataDetail extends MetadataBase{
    subjectArea:string;
    domain:string;
    subDomain:string;
    primaryBsaSae:string;
    primaryDevSae:string;
    teamDame:string;
    teamDl:string;
    pmManager:string;
    devManager:string;
    productOwner:string;
    hdfsLocation:string;
    distinctBatchCnt:number;
    batchAccessCnt:number;
    distinctUserCnt:number;
    userAccessCnt:number;
    frequency:string;
    updTime:string;
    avgUpdTime:string;
    tableDesc:string;
    touchFile:string;
    column:Column[];
    view?:SubView[];
    table?:SubTable[]
    constructor(){
        super();
        this.subjectArea = '';
        this.domain = '';
        this.subDomain = '';
        this.primaryBsaSae = '';
        this.primaryDevSae = '';
        this.teamDame = '';
        this.teamDl = '';
        this.pmManager = '';
        this.devManager = '';
        this.productOwner = '';
        this.hdfsLocation = '';
        this.distinctBatchCnt = 0 ;
        this.batchAccessCnt = 0 ;
        this.distinctUserCnt = 0 ;
        this.userAccessCnt = 0 ;
        this.frequency = '';
        this.updTime = '';
        this.avgUpdTime = '';
        this.tableDesc = '';
        this.touchFile = '';
        this.column = [];
        this.table = [];
        this.view = [];
    }
}

interface SampleQuery{
    id?:string
    tableName:string
    platforms:string[]
    title:string
    desc:string
    createTime?:number
    creator?:string
    lastUpdateTime?:number
    lastUpdate?:string
    show:boolean
    [k:string]:any
}
function mergeMetadata(details:Array<MetadataDetail>):MetadataDetail{
    let summary = _.reduce(details,(result,item) =>{
        if(item.tableDesc && item.tableDesc.length > result.tableDesc.length){
            result.tableDesc = item.tableDesc
        }
        if(!result.productOwner && item.productOwner){
            result.productOwner = item.productOwner
        }

        if(!result.teamDl && item.teamDl){
            result.teamDl = item.teamDl
        }
        if(!result.subjectArea && item.subjectArea){
            result.subjectArea = item.subjectArea;
        }
        
        return result;
    },new MetadataDetail())
    return summary;
} 



function getAllItems(dict:Dict<Dict<MetadataDetail>>,sort:boolean = false):MetadataDetail[]{
    let result =  _.chain(dict).map((subDict:Dict<MetadataDetail>,plat:string) => {
        let details:MetadataDetail[] = _.chain(subDict).reduce( function(r:MetadataDetail[], detail:MetadataDetail, db:string) {
            r.push(detail)
            return r;
          }, []).value();
        return details
    }).reduce((r:MetadataDetail[], ds:MetadataDetail[]) => {
        r = _.union(r,ds)
        return r;
      }, []).value()
    if(!sort){
        return result;
    }
    result = _.sortBy(result,d => d.distinctBatchCnt + d.distinctUserCnt)
    return result;
}
function groupByPlatAndDb(details:MetadataDetail[]):Dict<Dict<MetadataDetail>>{
    let result:Dict<Dict<MetadataDetail>> = {};
    let dByPlat = _.chain(details).groupBy("platform").value();
    _.forEach(dByPlat,(details:MetadataDetail[],plat:string)=>{
        if(!result[plat]){
            result[plat] = {}
        }
        let platItem = result[plat];
        _.forEach(details,(d) => {
            const dbName = (d.system.dbName || 'DEFAULT').toUpperCase();
            platItem[dbName] = d;
        })

    })
    return result;
}
function getDetailDetail(details:MetadataDetail[],platform?:platform,database?:string):MetadataDetail{

    const sortedDetails = _.sortBy(details,d => d.distinctBatchCnt + d.distinctUserCnt)
    return sortedDetails[0]
}
function parseToMetadataDetail(data:any[]):MetadataDetail[]{
    return _.map(data, (d) => {
        let detail:MetadataDetail = <MetadataDetail>{
            ...d
        }
        detail.system = {
            platform:d.platform,
            dbName: d.dbName
        }
        return detail
    })

}
function mergerMetadataFromDoe(metadataList:MetadataDetail[],srcList:any[]){
    _.forEach(metadataList, (meta) => {
        let p = meta.system.platform;
        let db = meta.system.dbName;
        let name = meta.name;
        let src = _.find(srcList, src => src.platform == p && src.db_name == db && src.table_name == name);
        if(src){
            meta.updTime = src.upd_time;
            meta.userAccessCnt = src.user_access_cnt;
            meta.batchAccessCnt = src.batch_access_cnt;
            meta.distinctUserCnt = src.distinct_user_cnt;
            meta.distinctBatchCnt = src.distinct_batch_cnt;
        }
    })
}

function getDefaultDetail(dict:Dict<Dict<MetadataDetail>>,platform?:platform,database?:string):MetadataDetail{
    let details = getAllItems(dict,true)
    // console.log("array",details)
    if(platform){
        details = details.filter(d => d.system.platform == platform)
    }
    if(database) {
        details = details.filter(d => d.system.dbName == database)
    }
    const sortedDetails = _.sortBy(details,d => d.distinctBatchCnt + d.distinctUserCnt)
    return sortedDetails[0]
}
function mergeByPlat(dict:Dict<Dict<MetadataDetail>>){
    let dictByPlatform:Dict<MetadataDetail> = {};
    _.forEach(dict,(d:Dict<MetadataDetail>,plat:string)=>{
        let views:SubView[] = [];
        let tables:SubTable[] = [];

        _.forEach(d,(metadata,db) => {
            let vs = _.filter(metadata.view, v => v && v.viewName && v.viewName.toUpperCase() === metadata.name.toUpperCase()) as any as SubView[];
            let ts = metadata.table;      
            if(vs && vs.length > 0){
                views = _.unionWith(views,vs,_.isEqual)
            }
            if(ts && ts.length > 0){
                tables = _.unionWith(tables,ts,_.isEqual)
            }
        })
        let dbName = _.keys(d)[0];
        let detail = _.cloneDeep(d[dbName])
        if(views.length > 0){
            detail.view = views;
        }
        if(tables.length > 0){
            detail.table = tables;
        }
        dictByPlatform[plat] = detail;
    })
    // console.log("dictByPlatform",dictByPlatform)
    return dictByPlatform;

}
function getDatabaseOptions(dict:Dict<Dict<MetadataDetail>>){
    let dbs:string[] = [];
    _.chain(dict).forEach((detail:Dict<MetadataDetail>,platForm:string)=>{
        _.forEach(detail,(metadata:MetadataDetail) => {
            let dbName = metadata.system.dbName.toUpperCase() || 'DEFAULT';
            if(!_.includes(dbs,dbName))
            dbs.push(dbName)
        })
    }).value()
    return dbs
}
const injectSearchEvent = (cols:ColumnWrapper[]) => {
    const src: any[] = [];
    cols.forEach(c => {
        const srcObj = src.find(s => s.col === c.columnName)
        if(srcObj){
            c.columnDesc = srcObj.desc
            c.modified = true;
        }
    })
}
function getAllColumns(dict:Dict<Dict<MetadataDetail>>):ColumnWrapper[]{
    // get all columns 
    let allColumn:Column[] = [];
    _.chain(dict).forEach((detail:Dict<MetadataDetail>,platForm:string)=>{
        _.forEach(detail,(metadata:MetadataDetail,db:string) => {
            let cols = _.cloneDeep(metadata.column);
            cols = _.map(cols,(c) => {
                c.platform = platForm;
                c.database = db;
                c.columnName = c.columnName.trim().toUpperCase();
                return c
            })
            allColumn = _.concat(allColumn,cols)
        })
    }).value();
    //merge columns
    let colDict = _.chain(allColumn).groupBy('columnName').value() as any as Dict<Column>
    let allColumnsInfo =  _.map(colDict, (cols:Column[],colName:string) => {
        let resultTemplate = <ColumnWrapper>{
            columnName:colName,
            columnId:-1,
            columnDesc:"",
            dataType:"",
            dbMap:{},
            dataTypeMap:{},
            platforms:[]
        }
        let result:ColumnWrapper = _.reduce(cols,(r:ColumnWrapper,col:Column,key:number)=>{
            if(r.columnId && r.columnId < 0){
                r.columnId = col.columnId;
            }
            // merge desc 
            if(col.columnDesc && col.columnDesc.length > r.columnDesc.length){
                r.columnDesc = col.columnDesc
            }
            
            const platform = col.platform;
            const database = (col.database || 'DEFAULT').toUpperCase();
            let dbMap = r.dbMap;
            // set dbMap
            if(dbMap[platform]){
                if(!_.includes(dbMap[platform],database)){
                    dbMap[platform].push(database)
                }
            }
            else {
                let dbs = [database]
                dbMap[platform] = dbs
            }
            // set dataTypeMap
            let dataTypeMap = r.dataTypeMap;
            if(!dataTypeMap[platform]){
                dataTypeMap[platform] = {}
            }
            if(!dataTypeMap[platform][database]){
                dataTypeMap[platform][database] = col.dataType
            }
            // set platform 
            if(!_.includes(r.platforms,platform)){
                r.platforms.push(platform)
            }
            return r
        },resultTemplate)
        return result;
    }) as any as ColumnWrapper[]
    // console.log("allColumnsInfo",allColumnsInfo)
    // injectSearchEvent(allColumnsInfo)
    return allColumnsInfo


}
function detailDictForEach(dict:Dict<Dict<MetadataDetail>>,func:(meta:MetadataDetail,platForm:string,database:string) => void){
    _.forEach(dict, (subDict:Dict<MetadataDetail>,p:string) => {
        _.forEach(subDict,(detail,db:string) => {
            func(detail,p,db)
        })
    })
}
function getViewFullname(v:SubView){
    return `${v.viewDb.toUpperCase() || 'DEFAULT'}.${v.viewName.toUpperCase()}`
}
function getTableFullname(t:SubTable){
    return `${t.dbName.toUpperCase() || 'DEFAULT'}.${t.dbName.toUpperCase()}`
}
export {
    SystemInfo,
    MetadataBase,
    MetadataSubView,
    SubView,
    SubTable,
    Column,
    ColumnWrapper,
    SampleQuery,
    MetadataDetail,
    mergeMetadata,
    getAllItems,
    groupByPlatAndDb,
    getDetailDetail,
    getDefaultDetail,
    parseToMetadataDetail,
    mergeByPlat,
    getDatabaseOptions,
    getAllColumns,
    detailDictForEach,mergerMetadataFromDoe
}
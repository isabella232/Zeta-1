import DoeRemoteService from "@/services/remote/DoeRemoteService";
import XLSX from 'xlsx';
import _ from "lodash";

export {
    downloadUsage,
    exportExcel
}

function downloadUsage(params: any) {
    const doeRemoteService = new DoeRemoteService();
    doeRemoteService.getContactList(params).then((res: any) => {
        console.debug("Call Api:getMetaDataTable success");
        if (res && res.data && res.data.data && res.data.data.value) {
            exportUsage(params, res.data.data.value);
        }
    }).catch(err => {
        console.error("Call Api:getMetaDataTable failed: " + JSON.stringify(err));
    });
}

function exportUsage(params: any, data: any) {
    const exportData: any = _.transform(data, (rs: any, v: any, k: any) => {
        let arr: any = [];
        arr.push(_.upperFirst(_.toLower(v.platform).replace("numozart", "mozart")));
        arr.push((_.isEmpty(v.batch_acct) ? v.user_nt : v.batch_acct) + (v.status == 'inactive' ? '(inactive)' : ''));
        arr.push(v.status == 'inactive' ? '' : v.user_name);
        arr.push(_.isEmpty(v.batch_acct) ? "user" : "batch");
        arr.push(v.status == 'inactive' ? '' : v.user_mail);
        rs.push(arr);
    }, []);
    exportData.splice(0, 0, ['Platform', 'Account', 'User Name/Batch Owner Name', 'Type', 'Email']);
    /* convert state to workbook */
    const ws: any = XLSX.utils.aoa_to_sheet(_.concat(exportData));
    const wb: any = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Usage");
    /* generate file and send to client */
    XLSX.writeFile(wb, params.table + "_usage.xls");
}

function exportExcel(data: any) {
    /* convert state to workbook */
    const ws: any = XLSX.utils.aoa_to_sheet(_.concat(data));
    const wb: any = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "download");
    /* generate file and send to client */
    XLSX.writeFile(wb, "download.xlsx");
}
import DssAPI from '@/services/remote/dss-api-remote-service';
import config from '@/config/config';
const baseUrl = config.zeta.base;
export default class PackageRemoteService extends DssAPI {
  // ==================== package ===================
  uploadPackage (form: any, path: string) {
    const formData = new FormData();
    formData.append('file', form.fileList[0].raw);
    formData.append('fileName', form.fileList[0].fileName+'.'+form.fileList[0].fileType);
    formData.append('filePath', path);
    formData.append('fileType', form.fileList[0].fileType);
    formData.append('cluster', form.cluster);
    formData.append('isOverWrite', form.isOverWrite);
    const config = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      // 'onUploadProgress': (progressEvent: any) => {
      //     if (progressEvent.lengthComputable) {
      //         let val = (progressEvent.loaded / progressEvent.total * 100).toFixed(0);
      //         console.log('uploadfile progress',parseInt(val))
      //     }
      // }
    };
    return this.post(`${baseUrl}package/upload`, formData,  config);
  }
  rename (id: number, name: string) {
    return this.put(`${baseUrl}package/${id}/rename?newName=${name}`);
  }

  del (id: number) {
    return this.delete(`${baseUrl}package/${id}`);
  }
  getAllPackage (){
    return this.get(`${baseUrl}package/list`);
  }
  getHDFSFile (cluster: number, path: string){
    return this.get(`${baseUrl}package/list?cluster=${cluster}&path=${path}`);
  }
}

import DssAPI from '@/services/remote/dss-api-remote-service';
import config from '@/config/config';
const baseUrl = config.zeta.base;
export default class HDFSRemoteService extends DssAPI {
  // ==================== package ===================
  upload (form: any, path: string, onProgress: (e: ProgressEvent) => void) {
    const formData = new FormData();
    formData.append('file', form.fileList[0].raw);
    formData.append('fileName', form.fileList[0].fileName + '.' + form.fileList[0].fileType);
    formData.append('filePath', path);
    // formData.append('fileType', form.fileList[0].fileType);
    formData.append('cluster', form.cluster);
    formData.append('isOverWrite', form.isOverWrite);
    const config = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      'onUploadProgress': onProgress,
    };
    return this.post(`${baseUrl}hdfs/upload`, formData, config);
  }
  create (cluster: number, path: string){
    return this.put(`${baseUrl}hdfs/create?cluster=${cluster}&filePath=${path}`);
  }
  download (cluster: number, path: string){
    return this.get(`${baseUrl}hdfs/read?cluster=${cluster}&filePath=${path}`, {responseType: 'blob'});
  }
  rename (srcPath: string, dstPath: string, cluster: number) {
    return this.put(`${baseUrl}hdfs/rename?srcPath=${srcPath}&dstPath=${dstPath}&cluster=${cluster}`);
  }

  del (cluster: number, path: string) {
    return this.delete(`${baseUrl}hdfs/delete?cluster=${cluster}&filePath=${path}`);
  }
  getAllPackage () {
    return this.get(`${baseUrl}hdfs/list`);
  }
  getHDFSFile (cluster: number, path: string) {
    return this.get(`${baseUrl}hdfs/list?cluster=${cluster}&path=${path}`);
  }
}

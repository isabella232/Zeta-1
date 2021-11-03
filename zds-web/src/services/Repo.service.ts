import { RestPacket } from '@/types/RestPacket';
import { IFile } from '@/types/repository';
class RepoService {
  file(f: RestPacket.File): IFile{
    const path: string = f.path || '/';
    return {
      path: path,
      notebookId: f.id,
      nt: f.nt,
      content: f.content,
      title: f.title,
      createTime: f.createDt,
      updateTime: f.updateDt ? f.updateDt : f.createDt,
      lastRunTime: f.lastRunDt,
      state: f.status,
      preference: f.preference
        ? JSON.parse(f.preference)
        : undefined,
      selected: false,
      nbType: f.nbType || 'single',
      favorite: false,
    };
  }
}
export const RepoSrv = new RepoService();
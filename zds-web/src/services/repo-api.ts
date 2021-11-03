import NotebookRemoteService from "@/services/remote/NotebookRemoteService";
import ZeppelinApi from "@/services/remote/ZeppelinApi";
import { IFile } from "@/types/repository";
import { IRenameOption } from "@/plugins/repo/rename-file-dialog";
import { IPacketMapper } from "@/services/mapper";
import { ZPLNote, NotebookType } from "@/types/workspace";
import _ from "lodash";
import Util from "./Util.service";
import { ZetaException } from "@/types/exception";
export interface IRepoApi {
	createNotebook: Function;
	renameNotebook: Function;
	deleteNotebook: Function;
}
export default class RepoApiHelper implements IRepoApi{
	private zetaApi: NotebookRemoteService;
	private zeppelinApi: ZeppelinApi;

	constructor(zetaApi: NotebookRemoteService, zeppelinApi: ZeppelinApi) {
		this.zetaApi = zetaApi;
		this.zeppelinApi = zeppelinApi;
	}
	
	public renameNotebook(file: IFile, option: IRenameOption): Promise<IFile> {
		let newFile: IFile = {
			...file,
			title: option.name,
			path: option.path
		}
		if(file.nbType === 'zeppelin') {
			let title = newFile.path + newFile.title
			return this.zeppelinApi.renameNote(newFile.notebookId, title).then(() => {
				return newFile
			});
		} else {
			return this.zetaApi.rename(IPacketMapper.fileMapper(newFile)).then(() => {
				return newFile
			})
		}
	};
	public createNotebook(file: IFile): Promise<IFile> {
		let result = _.clone(file)
		if(file.nbType === 'zeppelin') {
			let note: ZPLNote = <ZPLNote>{
				name: file.path + file.title
			  }
			return this.zeppelinApi.createNote(note).then(({data}) => {
				let id = data.body;
				result.notebookId = id;
				result.nbType = 'zeppelin';
				return result;
			}).catch(e => {
				if(e.orginalResponse) {
					const response = e.orginalResponse;
					const isDuplicateName = response.data.status === 'e.orginalResponse' && response.data.message === 'duplicate file name';
					if(isDuplicateName) {
						e.code = 'EXIST_NOTEBOOK';
					}
				}
				throw e;
			});
		} else {
			return this.zetaApi.rename(IPacketMapper.fileMapper(file)).then(({data}) => {
				let id = data.id;
				let preference = data.preference
				? JSON.parse(data.preference)
				: undefined;
				result.notebookId = id;
				result.preference = preference;
				return result
			})
		}
	}
	public deleteNotebook(file: IFile): Promise<any> {
		if(file.nbType === 'zeppelin') {
			return this.zeppelinApi.delNote(file.notebookId).catch((e: ZetaException) => {
				e.message = `Fail to delete ${Util.getPathname(file)} on server`;  
			});
		} else {
			return this.zetaApi.del(file.notebookId).catch((e: ZetaException) => {
				e.message = `Fail to delete ${Util.getPathname(file)} on server`;  
			});
		}
	}
	public deleteFolder(path: string,containsZetaFile: boolean, containsZeppelinFile: boolean) {
		const promises: Promise<any>[] = []
		if(containsZetaFile) {
			promises.push(this.zetaApi.removeFolder(path));
		}
		if(containsZeppelinFile) {
			let folderId = path;
			if(_.startsWith(folderId, '/')) {
				folderId = folderId.substr(1, folderId.length - 1);
			}
			if(_.endsWith(folderId, '/')) {
				folderId = folderId.substr(0, folderId.length - 1);
			}
			promises.push(this.zeppelinApi.removeFolder(folderId))
		}
		if(promises.length > 0) {
			return Promise.all(promises)
		} else {
			return new Promise((r) => {
				r();
			})
		}
	}

}
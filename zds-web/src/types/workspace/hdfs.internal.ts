interface IHDFSFile {
	fileName: string;
	filePath: string;
	isFile: boolean;
	type?: string | null;
	createTime?: number;
	updateTime?: number;
  selectable: boolean;
  cluster?: number;
}
interface IFile {
	path: string;
	isFile: boolean;
	accessTime: number;
	modifyTime: number;
	owner: string;
	permission: number;
}
interface IPackageFile {
	id: number;
	nt: string;
	fileName: string;
	filePath: string;
    cluster: number;
    platform?: string;
	type: string;
	createTime: number;
	updateTime?: number;
}
export { IHDFSFile, IPackageFile,IFile };

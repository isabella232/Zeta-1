import { IPreference, INotebook, NotebookType } from '@/types/workspace';
import { IPackage } from './workspace/notebook.internal';

/* Repository types */
export interface IFile {
  notebookId: string;
  nt: string;
  content?: string;
  path: string;
  title: string;
  /* TODO: format */
  createTime: number;
  updateTime: number;
  lastRunTime?: number;
  /* TODO: Type? */
  state: string;
  preference: IPreference | undefined;
  /* -- */
  selected: boolean;
  opened?: number;
  seq?: number;

  nbType: NotebookType;
  collectionId?: string;
  subNotebooks?: INotebook[];
  packages?: IPackage;
  favorite: boolean;
}

export interface IRepositoryStore {
  files: Dict<IFile>;
  updateTime: string;
  loading: boolean;
}


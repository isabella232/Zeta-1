export interface PublicNotebookItem {
  id: string;
  title: string;
  nt?: string;
  createDt: number;
  updateDt: number;
  platform: string;
  interpreter: string;
  isZeppelin?: boolean;
  path: string;
}

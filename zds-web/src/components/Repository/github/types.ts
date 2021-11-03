export interface GithubFile {
    fullPath: string
    path: string
    type: 'FILE_NORMAL' | 'FOLDER',
    sha: string,
    selected: boolean,
    subFiles? : GithubFile[]
}
export interface ZetaFile {
    fullPath: string
    title: string
    notebookId: string
    selected: boolean,
    errorMsg?: string
}
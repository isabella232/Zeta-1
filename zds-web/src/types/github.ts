interface Github {
    nt: string
    url?: string
    branch?: string
    newBranch?: string
    folderSha?: string
    wsPath?: string
    tag?: string
    searchKeyword?: string
    commitMessage?: string
    fileList?: Dict<string>[]
}
export {
    Github
}
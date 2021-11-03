package com.ebay.dss.zds.model;

import java.util.List;

public class DeleteFolderRequest {

    private List<String> folders;
    private boolean recursive = false;

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
        this.folders = folders;
    }
}

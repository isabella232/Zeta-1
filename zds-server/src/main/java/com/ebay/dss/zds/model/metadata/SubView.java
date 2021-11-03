package com.ebay.dss.zds.model.metadata;

/**
 * Created by tianrsun on 2018/11/6.
 */
public class SubView {
    private String viewDb;
    private String viewName;
    private boolean disable;
    public SubView(){
        this.disable = false;
    }
    public String getViewDb() {
        return viewDb;
    }

    public void setViewDb(String viewDb) {
        this.viewDb = viewDb;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }
}

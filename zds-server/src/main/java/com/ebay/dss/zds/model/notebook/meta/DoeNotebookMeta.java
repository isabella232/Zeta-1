package com.ebay.dss.zds.model.notebook.meta;


public class DoeNotebookMeta {

    private String id;
    private String desc;
    private String type;
    private String title;
    private String tables;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTables() {
        return tables;
    }

    public void setTables(String tables) {
        this.tables = tables;
    }

    public static DoeNotebookMeta fromProjection(DoeNotebookMetaProjection projection) {
        DoeNotebookMeta meta = new DoeNotebookMeta();
        meta.setId(projection.getId());
        meta.setDesc(projection.getDesc());
        meta.setTitle(projection.getType());
        meta.setTitle(projection.getTitle());
        meta.setTables(projection.getReference());
        return meta;
    }
}

package com.ebay.dss.zds.model.notebook.meta;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "zeta_notebook_meta")
public class NotebookMeta {
    @Id
    @Column
    private String id;

    @Column(name = "is_public")
    private String isPublic;

    @Column(name = "description")
    private String desc;

    @Column(name = "notebook_type")
    private String type;

    @Column
    private String reference;
    
    private Date createTime;
    
    private Date updateTime;

    public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(String isPublic) {
        this.isPublic = isPublic;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

	public boolean equals(NotebookMeta meta) {
		return meta.getDesc().equals(desc)
				&& meta.getIsPublic().equals(isPublic)
				&& meta.getReference().equals(reference)
				&& meta.getType().equals(type);
	}
    
}
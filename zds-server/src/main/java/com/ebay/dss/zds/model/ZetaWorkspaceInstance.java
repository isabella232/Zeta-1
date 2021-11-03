package com.ebay.dss.zds.model;


import javax.persistence.*;
import javax.persistence.Column;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@IdClass(ZetaWorkspaceInstance.ZetaWorkspaceInstancePK.class)
@Table(name = "zeta_workspace_instance")
public class ZetaWorkspaceInstance {

  @Id
  private String id;

  @Id
  @Column
  private String nt;

  @Column
  private int seq;

  @Id
  @Column
  private String type;


  public ZetaWorkspaceInstance() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public static class ZetaWorkspaceInstancePK implements Serializable{
    private String id;
    private String nt;
    private String type;

    public ZetaWorkspaceInstancePK() {
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getNt() {
      return nt;
    }

    public void setNt(String nt) {
      this.nt = nt;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ZetaWorkspaceInstancePK that = (ZetaWorkspaceInstancePK) o;
      return Objects.equals(id, that.id) &&
          Objects.equals(nt, that.nt) &&
          Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, nt, type);
    }
  }
}

package com.ebay.dss.zds.model;

import javax.persistence.*;
import javax.persistence.Column;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@IdClass(ZetaFavorite.ZetaFavoritePK.class)
@Table(name = "zeta_favorite")
public class ZetaFavorite {
    @Id
    @Column
    private String id;

    @Id
    @Column
    private String nt;

    @Id
    @Column(name = "favorite_type")
    private String favoriteType;

    @Column(name = "update_dt")
    private Date updateDt;

    @Column
    private String favorite;

    public ZetaFavorite() {
    }

    public ZetaFavorite(String id, String nt, String favoriteType, Date updateDt, String favorite) {
        this.id = id;
        this.nt = nt;
        this.favoriteType = favoriteType;
        this.updateDt = updateDt;
        this.favorite = favorite;
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

    public String getFavoriteType() {
        return favoriteType;
    }

    public void setFavoriteType(String favoriteType) {
        this.favoriteType = favoriteType;
    }

    public Date getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Date updateDt) {
        this.updateDt = updateDt;
    }

    public String getFavorite() {
        return favorite;
    }

    public void setFavorite(String favorite) {
        this.favorite = favorite;
    }

    public static class ZetaFavoritePK implements Serializable {
        private String id;
        private String nt;
        private String favoriteType;

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

        public String getFavoriteType() {
            return favoriteType;
        }

        public void setFavoriteType(String favoriteType) {
            this.favoriteType = favoriteType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ZetaFavoritePK that = (ZetaFavoritePK) o;
            return Objects.equals(id, that.id) &&
                Objects.equals(nt, that.nt) &&
                Objects.equals(favoriteType, that.favoriteType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, nt, favoriteType);
        }
    }
}

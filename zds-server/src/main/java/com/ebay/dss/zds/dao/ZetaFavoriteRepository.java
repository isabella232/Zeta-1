package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ZetaFavoriteRepository extends JpaRepository<ZetaFavorite, ZetaFavorite.ZetaFavoritePK> {

    @Transactional
    @Modifying
    @Query(value = "replace into zeta_favorite values (?1, ?2, ?3, now(), '1')", nativeQuery = true)
    void favoriteNote(String id, String nt, String favoriteType);

    @Transactional
    @Modifying
    @Query(value = "replace into zeta_favorite values (?1, ?2, ?3, now(), '0')", nativeQuery = true)
    void unFavoriteNote(String id, String nt, String favoriteType);

    List<ZetaFavorite> findAllByNtAndFavoriteIs(String nt, String favorite);

    List<ZetaFavorite> findAllByNtAndFavoriteAndFavoriteTypeIs(String nt, String favorite, String favoriteType);

    List<ZetaFavorite> findAllByNtAndFavoriteAndFavoriteTypeIn(String nt, String favorite, List<String> favoriteTypes);

}

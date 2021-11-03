package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AlationArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlationArticleRepository extends JpaRepository<AlationArticle, Integer> {

    Optional<AlationArticle> findOneByUrl(String url);

}

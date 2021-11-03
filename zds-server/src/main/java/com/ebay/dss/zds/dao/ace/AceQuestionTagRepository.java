package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AceQuestionCountByTag;
import com.ebay.dss.zds.model.ace.AceQuestionTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AceQuestionTagRepository extends JpaRepository<AceQuestionTag, AceQuestionTag.AceQuestionTagId> {

    @Query(value = "select new com.ebay.dss.zds.model.ace.AceQuestionCountByTag(qt.tagId, count(qt)) " +
            "from AceQuestionTag qt " +
            "group by qt.tagId " +
            "order by count(qt) desc")
    List<AceQuestionCountByTag> countHotTagIds(Pageable pageable);

    List<AceQuestionTag> findByQuestionIdIn(Iterable<Integer> questionIds);

    @Query(value = "select qt " +
            "from AceQuestionTag qt " +
            "join AceQuestion q on q.id = qt.questionId " +
            "where qt.tagId in :tagIds " +
            "group by qt.questionId " +
            "having count(qt) >= :tagIdsLen")
    Page<AceQuestionTag> findByTagIdIn(Iterable<Integer> tagIds, long tagIdsLen, Pageable pageable);

    void deleteAllByQuestionIdAndTagIdIn(Integer questionId, Iterable<Integer> tagIds);
}

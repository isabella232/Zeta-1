package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AceQuestionLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AceQuestionLikeRepository extends JpaRepository<AceQuestionLike, Integer> {

    Optional<AceQuestionLike> getOneByNtAndQuestionId(String nt, Integer questionId);

    List<AceQuestionLike> findAllByQuestionId(Integer questionId);

    List<AceQuestionLike> findAllByQuestionIdIn(Iterable<Integer> questionIds);

    Integer deleteByQuestionId(Integer questionId);
}

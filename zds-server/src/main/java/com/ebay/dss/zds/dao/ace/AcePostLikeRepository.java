package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AcePostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcePostLikeRepository extends JpaRepository<AcePostLike, Integer> {

    Optional<AcePostLike> getOneByNtAndQuestionIdAndPostId(String nt, int questionId, Integer postId);

    List<AcePostLike> findAllByQuestionId(int questionId);

    List<AcePostLike> findAllByQuestionIdIn(Iterable<Integer> questionIds);

    Integer deleteByQuestionId(Integer questionId);

    Integer deleteByPostId(Integer postId);

    @Modifying
    @Query("delete from AcePostLike where postId in (select p.id from AceQuestionPost p where p.replyTo=:replyTo)")
    Integer deleteByReplyTo(Integer replyTo);
}

package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AceQuestionPost;
import com.ebay.dss.zds.model.ace.CountResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AceQuestionPostRepository extends JpaRepository<AceQuestionPost, Integer> {

    @Query(nativeQuery = true, value = "select " +
            "question_id as id, count(*) as count " +
            "from zeta_ace_question_post " +
            "where question_id in :questionIds group by question_id")
    List<CountResult> countPerQuestion(List<Integer> questionIds);

    @Query(nativeQuery = true, value = "select " +
            "question_id as id, count(*) as count " +
            "from zeta_ace_question_post " +
            "where question_id in :questionIds and reply_to is null group by question_id")
    List<CountResult> countLevel1PostPerQuestion(List<Integer> questionIds);

    List<AceQuestionPost> findAllByQuestionId(Integer questionId);

    List<AceQuestionPost> findAllByQuestionIdIn(Iterable<Integer> questionIds);

    Boolean existsByIdAndQuestionIdAndPoster(Integer postId, Integer questionId, String nt);

    Boolean existsByIdAndQuestionId(Integer postId, Integer questionId);

    Optional<AceQuestionPost> findByIdAndPoster(Integer postId, String nt);

    Optional<AceQuestionPost> findByIdAndPosterAndQuestionId(Integer postId, String nt, Integer questionId);

    /**
     * This method combines with delete to avoid foreign key constraint.
     * @param questionId
     * @return
     */
    @Modifying
    @Query("update AceQuestionPost set replyTo = null where questionId = :questionId")
    Integer removeReplyTo(Integer questionId);

    Integer deleteByQuestionId(Integer questionId);

    Integer deleteByReplyTo(Integer replyTo);

    @Modifying
    @Query("update AceQuestionPost set accepted = :value where questionId = :questionId and id = :postId")
    Integer setAccepted(Integer questionId, Integer postId, Boolean value);
}

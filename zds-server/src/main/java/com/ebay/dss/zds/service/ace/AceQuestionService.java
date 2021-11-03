package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.*;

import java.util.List;
import java.util.Optional;

public interface AceQuestionService {

    AceQuestion createQuestion(AceQuestion question);

    AceQuestion editQuestion(AceQuestion question, String editor);

    Optional<AceQuestionDto> getQuestion(Integer questionId, String nt);

    List<AceQuestionDto> getQuestions(Iterable<Integer> ids, String nt);

    AceQuestionPost createQuestionPost(AceQuestionPost post);

    AceQuestionsDto listQuestions(ListQuestionOptions options);

    Boolean acceptPost(Integer questionId, Integer postId);

    Boolean revokeAcceptPost(Integer questionId, Integer postId);

    Boolean pickUpQuestion(Integer questionId, Integer score);

    Boolean revokePickUpQuestion(Integer questionId);

    /**
     *
     * @param nt
     * @param questionId
     * @param postId
     * @param flag 1 is like, 0 is nothing, -1 is dislike
     * @return
     */
    AcePostLike votePost(String nt, Integer questionId, Integer postId, Integer flag);

    AceQuestionLike voteQuestion(String nt, Integer questionId, Integer flag);

    AceQuestionPost editQuestionPost(AceQuestionPost post, String editor);

    Boolean deleteQuestion(Integer questionId, String nt);

    Boolean deletePost(Integer questionId, Integer postId, String nt);

    Boolean deleteQuestion(Integer questionId);

    Boolean deletePost(Integer questionId, Integer postId);
}

package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.ace.AceQuestion;
import com.ebay.dss.zds.model.ace.AceQuestionPost;
import com.ebay.dss.zds.model.ace.AceQuestionTagsMap;
import com.ebay.dss.zds.model.ace.AceTag;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.rest.wrapper.BooleanResult;
import com.ebay.dss.zds.service.ace.AceQuestionService;
import com.ebay.dss.zds.service.ace.AceTagService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

@RestController
@RequestMapping("/ace/admin")
@PreAuthorize("hasRole('ACE_ADMIN')")
public class AceAdminController {

    private final AceQuestionService questionService;
    private final AceTagService tagService;

    public AceAdminController(AceQuestionService questionService, AceTagService tagService) {
        this.questionService = questionService;
        this.tagService = tagService;
    }

    @PostMapping("/questions")
    public ZetaResponse<AceQuestion> updateQuestion(@AuthenticationNT String adminNt, @RequestBody AceQuestion question) throws IOException {
        return ZetaResponse.success(questionService.editQuestion(question, adminNt));
    }


    @DeleteMapping("/question/{qid}")
    public ZetaResponse<BooleanResult> deleteQuestion(@PathVariable("qid") Integer questionId) {
        return ZetaResponse.success(new BooleanResult(questionService.deleteQuestion(questionId)));
    }

    @PostMapping("/question/{qid}/posts")
    public ZetaResponse<AceQuestionPost> updatePost(
            @AuthenticationNT String adminNT,
            @RequestBody AceQuestionPost post) {
        return ZetaResponse.success(questionService.editQuestionPost(post, adminNT));
    }

    @DeleteMapping("/question/{qid}/post/{pid}")
    public ZetaResponse<BooleanResult> deletePost(
            @PathVariable("qid") Integer questionId,
            @PathVariable("pid") Integer postId) {
        return ZetaResponse.success(new BooleanResult(questionService.deletePost(questionId, postId)));
    }

    @PutMapping("/tags")
    public ZetaResponse<AceTag> findOrCreateTag(@RequestBody AceTag tag) {
        return ZetaResponse.success(tagService.updateTag(tag));
    }

    @PutMapping("/question/{qid}/tags/{tagIds}")
    public ZetaResponse<AceQuestionTagsMap> addQuestionTags(
            @PathVariable(value = "qid") Integer questionId,
            @PathVariable Integer[] tagIds
    ) {
        tagService.addTagsToQuestionId(new HashSet<>(Arrays.asList(tagIds)), questionId);
        return ZetaResponse.success(tagService.findTagsByAceQuestionId(questionId));
    }

    @DeleteMapping("/question/{qid}/tags/{tagIds}")
    public ZetaResponse<AceQuestionTagsMap> removeQuestionTags(
            @PathVariable(value = "qid") Integer questionId,
            @PathVariable Integer[] tagIds
    ) {
        tagService.removeTagsFromQuestionId(new HashSet<>(Arrays.asList(tagIds)), questionId);
        return ZetaResponse.success(tagService.findTagsByAceQuestionId(questionId));
    }

    @PutMapping("/question/{qid}/post/{pid}/accepted")
    public ZetaResponse<BooleanResult> accept(@PathVariable("qid") Integer questionId,
                                              @PathVariable("pid") Integer postId) {
        return ZetaResponse.success(new BooleanResult(questionService.acceptPost(questionId, postId)));
    }

    @DeleteMapping("/question/{qid}/post/{pid}/accepted")
    public ZetaResponse<BooleanResult> revokeAccept(@PathVariable("qid") Integer questionId,
                                                    @PathVariable("pid") Integer postId) {
        return ZetaResponse.success(new BooleanResult(questionService.revokeAcceptPost(questionId, postId)));
    }

    @PutMapping("/question/{qid}/pickUp")
    public ZetaResponse<BooleanResult> pickUp(@PathVariable("qid") Integer questionId,
                                              @RequestParam(required = false, defaultValue = "1") Integer score) {
        return ZetaResponse.success(new BooleanResult(questionService.pickUpQuestion(questionId, score)));
    }

    @DeleteMapping("/question/{qid}/pickUp")
    public ZetaResponse<BooleanResult> revokePickUp(@PathVariable("qid") Integer questionId) {
        return ZetaResponse.success(new BooleanResult(questionService.revokePickUpQuestion(questionId)));
    }
}

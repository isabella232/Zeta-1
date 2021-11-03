package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.ace.*;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.rest.wrapper.BooleanResult;
import com.ebay.dss.zds.rest.wrapper.IntWrapper;
import com.ebay.dss.zds.service.ace.AceENotifyService;
import com.ebay.dss.zds.service.ace.AceQuestionService;
import com.ebay.dss.zds.service.ace.AceSearchService;
import com.ebay.dss.zds.service.ace.AceTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/ace")
public class AceController {

    private final AceENotifyService eNotifyService;
    private final AceSearchService searchService;
    private final AceQuestionService questionService;
    private final AceTagService tagService;

    @Autowired
    public AceController(@Qualifier("aceSearchMultiService") AceSearchService searchService,
                         AceQuestionService questionService,
                         AceENotifyService eNotifyService,
                         AceTagService tagService) {
        this.searchService = searchService;
        this.questionService = questionService;
        this.eNotifyService = eNotifyService;
        this.tagService = tagService;
    }

    @GetMapping("/search")
    public ZetaResponse<AceSearchEntry> search(AceSearchOptions options) {
        return ZetaResponse.success(searchService.search(options).orElse(null));
    }

    @GetMapping("/search/scopes")
    public ZetaResponse<Collection<String>> searchScopes() {
        return ZetaResponse.success(searchService.scopes());
    }

    @PostMapping("/questions")
    public ZetaResponse<AceQuestion> ask(@AuthenticationNT String nt,
                                         @RequestBody AceQuestion question) throws IOException {
        question.setSubmitter(nt);
        return ZetaResponse.success(questionService.createQuestion(question));
    }

    @GetMapping("/question/{qid}")
    public ZetaResponse<AceQuestionDto> getQuestion(@AuthenticationNT String nt,
                                                    @PathVariable("qid") Integer questionId) {
        return ZetaResponse.success(questionService.getQuestion(questionId, nt)
                .orElseThrow(() -> new EntityNotFoundException("Question not found")));
    }

    @GetMapping("/v1/question/{qids}")
    public ZetaResponse<List<AceQuestionDto>> getQuestion(@AuthenticationNT String nt,
                                                          @PathVariable("qids") Integer[] questionIds) {
        return ZetaResponse.success(questionService.getQuestions(Arrays.asList(questionIds), nt));
    }

    @DeleteMapping("question/{qid}")
    public ZetaResponse<BooleanResult> deleteQuestion(@AuthenticationNT String nt,
                                                      @PathVariable("qid") Integer questionId) {
        return ZetaResponse.success(new BooleanResult(questionService.deleteQuestion(questionId, nt)));
    }

    @PostMapping("/question/{qid}/posts")
    public ZetaResponse<AceQuestionPost> post(@AuthenticationNT String nt,
                                              @PathVariable("qid") Integer questionId,
                                              @RequestBody AceQuestionPost post) {
        post.setPoster(nt)
                .setQuestionId(questionId);
        return ZetaResponse.success(questionService.createQuestionPost(post));
    }

    @DeleteMapping("question/{qid}/post/{pid}")
    public ZetaResponse<BooleanResult> deletePost(@AuthenticationNT String nt,
                                                  @PathVariable("qid") Integer questionId,
                                                  @PathVariable("pid") Integer postId) {
        return ZetaResponse.success(new BooleanResult(questionService.deletePost(questionId, postId, nt)));
    }

    @GetMapping("/questions")
    public ZetaResponse<AceQuestionsDto> listQuestion(@AuthenticationNT String nt,
                                                      ListQuestionOptions options) {
        options.setNt(nt);
        return ZetaResponse.success(questionService.listQuestions(options));
    }

    @PutMapping("/question/{qid}/post/{pid}/vote")
    public Object votePost(@AuthenticationNT String nt,
                           @PathVariable("qid") Integer questionId,
                           @PathVariable("pid") Integer postId,
                           @RequestBody IntWrapper intWrapper) {
        return vote(nt, questionId, postId, intWrapper);
    }

    @PutMapping("/question/{qid}/vote")
    public Object voteQuestion(@AuthenticationNT String nt,
                               @PathVariable("qid") Integer questionId,
                               @RequestBody IntWrapper intWrapper) {
        return vote(nt, questionId, null, intWrapper);
    }

    private ZetaResponse<?> vote(String nt,
                                 Integer questionId,
                                 Integer postId,
                                 IntWrapper intWrapper) {
        int value = Optional.ofNullable(intWrapper.getValue()).orElse(1);
        int flag = (int) Math.signum(value);
        Object like = Objects.isNull(postId) ?
                questionService.voteQuestion(nt, questionId, flag) :
                questionService.votePost(nt, questionId, postId, flag);
        return ZetaResponse.success(like);
    }

    @GetMapping("/enotifies")
    public ZetaResponse<AceEnotifies> pollENotify(@AuthenticationNT String nt,
                                                  AceEnotifyOptions options) {
        if (options.getAfter() == null) {
            options.setAfter(LocalDate.now(ZoneId.of("UTC")).minus(2, ChronoUnit.DAYS));
        }
        if (options.getProducts() == null) {
            options.setProducts(new HashSet<>(Arrays.asList(AceEnotifyOptions.Product.values())));
        }
        options.setUser(nt);
        return ZetaResponse.success(eNotifyService.findEnotifies(options));
    }

    @PutMapping("/enotifies/read")
    public ZetaResponse<BooleanResult> checkENotify(@AuthenticationNT String nt,
                                                    @RequestBody AceEnotifyReadOptions checkOptions) {
        checkOptions.setUser(nt);
        return ZetaResponse.success(new BooleanResult(eNotifyService.setReadEnotifies(checkOptions)));
    }

    @GetMapping("/enotifies/products")
    public ZetaResponse<AceEnotifyOptions.Product[]> eNotifyProducts() {
        return ZetaResponse.success(eNotifyService.getAllEnotifyProducts());
    }

    @GetMapping("/enotifies/types")
    public ZetaResponse<AceEnotifyOptions.Type[]> enotifyTypes() {
        return ZetaResponse.success(eNotifyService.getAllEnotifyTypes());
    }

    @GetMapping("/hottags")
    public ZetaResponse<List<AceTag>> findHotTags(
            @RequestParam(defaultValue = "20") @Size(min = 1, max = 1000) int size
    ) {
        return ZetaResponse.success(tagService.findHotTags(size));
    }

    @GetMapping("/tags")
    public ZetaResponse<AceTagPage> findTags(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size,
            @RequestParam(required = false) String nameStart
    ) {
        return ZetaResponse.success(tagService.findTags(page, size, nameStart));
    }

    @PostMapping("/tags")
    public ZetaResponse<AceTag> findOrCreateTag(
            @AuthenticationNT String nt,
            @RequestBody AceTag tag
    ) {
        tag.setNt(nt);
        return ZetaResponse.success(tagService.findOrCreateTag(tag));
    }

    private <T> Set<T> arrayToSet(T[] array) {
        return new HashSet<>(Arrays.asList(array));
    }

    @GetMapping("/tag/{tagIds}")
    public ZetaResponse<List<AceTag>> findTags(
            @PathVariable Integer[] tagIds
    ) {
        return ZetaResponse.success(tagService.findByTagIds(arrayToSet(tagIds)));
    }

    @GetMapping("/tag/{tagIds}/questions")
    public ZetaResponse<AceTagQuestionsPage> findAceTagQuestions(
            @PathVariable Integer[] tagIds,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(50) int size
    ) {
        return ZetaResponse.success(tagService
                .findQuestionsByAceTagIds(arrayToSet(tagIds), page, size));
    }

    @GetMapping("/question/{questionIds}/tags")
    public ZetaResponse<AceQuestionTagsMap> findAceQuestionTags(
            @PathVariable Integer[] questionIds
    ) {
        return ZetaResponse.success(tagService
                .findTagsByAceQuestionIds(arrayToSet(questionIds)));
    }

    @PutMapping("/question/{qid}/tags/{tagIds}")
    public ZetaResponse<AceQuestionTagsMap> addQuestionTags(
            @AuthenticationNT String nt,
            @PathVariable(value = "qid") Integer questionId,
            @PathVariable Integer[] tagIds
    ) {
        tagService.addTagsToQuestionId(arrayToSet(tagIds), questionId, nt);
        return ZetaResponse.success(tagService.findTagsByAceQuestionId(questionId));
    }

    @DeleteMapping("/question/{qid}/tags/{tagIds}")
    public ZetaResponse<AceQuestionTagsMap> removeQuestionTags(
            @AuthenticationNT String nt,
            @PathVariable(value = "qid") Integer questionId,
            @PathVariable Integer[] tagIds
    ) {
        tagService.removeTagsFromQuestionId(arrayToSet(tagIds), questionId, nt);
        return ZetaResponse.success(tagService.findTagsByAceQuestionId(questionId));
    }
}

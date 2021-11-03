package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.common.BeanUtils;
import com.ebay.dss.zds.dao.ace.AcePostLikeRepository;
import com.ebay.dss.zds.dao.ace.AceQuestionLikeRepository;
import com.ebay.dss.zds.dao.ace.AceQuestionPostRepository;
import com.ebay.dss.zds.dao.ace.AceQuestionRepository;
import com.ebay.dss.zds.model.ace.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AceQuestionSimpleService implements AceQuestionService {

    private static final Logger logger = LogManager.getLogger();

    private AceQuestionRepository questionRepository;
    private AceQuestionPostRepository postRepository;
    private AcePostLikeRepository postLikeRepository;
    private AceQuestionLikeRepository questionLikeRepository;
    private Predicate<AceQuestionPostDto> level1PostPredict = postDto -> postDto.getReplyTo() == null;
    private Predicate<AceQuestionLike> questionLikePredict = like -> like.getFlag() > 0;
    private Predicate<AcePostLike> postLikePredict = like -> like.getFlag() > 0;

    @Autowired
    public AceQuestionSimpleService(AceQuestionRepository questionRepository,
                                    AceQuestionPostRepository postRepository,
                                    AcePostLikeRepository postLikeRepository,
                                    AceQuestionLikeRepository questionLikeRepository) {
        this.questionRepository = questionRepository;
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.questionLikeRepository = questionLikeRepository;
    }

    private static ZonedDateTime nowOfUtc() {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    @Override
    @Transactional
    public AceQuestion createQuestion(AceQuestion question) {
        AceQuestion question2Save = questionRepository.findByIdAndSubmitter(question.getId(), question.getSubmitter())
                .map(oldQuestion -> {
                    question.setCreateTime(null)
                            .setPickUp(null)
                            .setUpdateTime(nowOfUtc());
                    BeanUtils.copyPropertiesIgnoreNull(question, oldQuestion);
                    return oldQuestion;
                })
                .orElseGet(() -> question.setId(null)
                        .setPickUp(0)
                        .setCreateTime(nowOfUtc())
                        .setUpdateTime(nowOfUtc()));

        return questionRepository.save(question2Save);
    }

    @Override
    @Transactional
    public AceQuestion editQuestion(AceQuestion question, String editor) {
        AceQuestion question2Save = questionRepository.findById(question.getId())
                .map(oldQuestion -> {
                    question.setSubmitter(null)
                            .setCreateTime(null);
                    if (oldQuestion.getSubmitter().equals(editor)) {
                        question.setUpdateTime(nowOfUtc());
                    } else {
                        question.setEditor(editor)
                                .setEditTime(nowOfUtc());
                    }
                    BeanUtils.copyPropertiesIgnoreNull(question, oldQuestion);
                    return oldQuestion;
                }).orElseThrow(() -> new RuntimeException("Can not found question"));
        return questionRepository.save(question2Save);
    }

    @Override
    public Optional<AceQuestionDto> getQuestion(Integer id, String nt) {
        List<AceQuestionDto> questionVOs = getQuestions(Collections.singleton(id), nt);
        if (questionVOs.size() == 1) {
            return Optional.of(questionVOs.get(0));
        }
        return Optional.empty();
    }

    @Override
    public List<AceQuestionDto> getQuestions(Iterable<Integer> ids, String nt) {
        List<AceQuestion> questions = questionRepository.findAllById(ids);
        if (!questions.isEmpty()) {
            Map<Integer, List<AceQuestionPostDto>> postVOsMap = findPosts(ids);
            List<AceQuestionLike> questionLikes = questionLikeRepository.findAllByQuestionIdIn(ids);
            List<AcePostLike> postLikes = postLikeRepository.findAllByQuestionIdIn(ids);

            Map<Integer, Integer> likedByNtPerQuestion = likeByNtPerQuestion(questionLikes, nt);
            Map<Integer, Long> postLikePerQuestion = postLikePerQuestion(postLikes);
            Map<Integer, Long> likePerQuestion = likePerQuestion(questionLikes);
            Map<Integer, Long> likePerPost = likePerPost(postLikes);
            Map<Integer, Integer> likedByNtPerPost = likeByNtPerPost(postLikes, nt);

            return questions.stream()
                    .map(q -> {
                        AceQuestionDto questionDto = new AceQuestionDto();
                        BeanUtils.copyProperties(q, questionDto);

                        questionDto.setPosts(postVOsMap.getOrDefault(questionDto.getId(), Collections.emptyList()));
                        questionDto.setTotalPost((long) questionDto.getPosts().size());
                        questionDto.setTotalLevel1Post(questionDto.getPosts().stream().filter(level1PostPredict).count());
                        questionDto.setTotalPostLike(postLikePerQuestion.getOrDefault(questionDto.getId(), 0L));
                        questionDto.setTotalLike(likePerQuestion.getOrDefault(questionDto.getId(), 0L));
                        questionDto.setLiked(likedByNtPerQuestion.getOrDefault(questionDto.getId(), 0));
                        questionDto.getPosts()
                                .forEach(post -> {
                                    post.setTotalLike(likePerPost.getOrDefault(post.getId(), 0L));
                                    post.setLiked(likedByNtPerPost.getOrDefault(post.getId(), 0));
                                });

                        return questionDto;
                    })
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Map<Integer, Long> likePerQuestion(Collection<AceQuestionLike> likes) {
        return likes.stream()
                .filter(questionLikePredict)
                .collect(Collectors.groupingBy(AceQuestionLike::getQuestionId,
                        Collectors.counting()));
    }

    public Map<Integer, Long> postLikePerQuestion(Collection<AcePostLike> likes) {
        return likes.stream()
                .filter(postLikePredict)
                .collect(Collectors.groupingBy(AcePostLike::getQuestionId,
                        Collectors.counting()));
    }

    public Map<Integer, Long> likePerPost(Collection<AcePostLike> likes) {
        return likes.stream()
                .filter(postLikePredict)
                .collect(Collectors.groupingBy(AcePostLike::getPostId,
                        Collectors.counting()));
    }

    public Map<Integer, Integer> likeByNtPerQuestion(Collection<AceQuestionLike> likes, String nt) {
        return likes.stream()
                .filter(like -> Objects.equals(like.getNt(), nt))
                .collect(Collectors.toMap(
                        AceQuestionLike::getQuestionId,
                        AceQuestionLike::getFlag));
    }

    public Map<Integer, Integer> likeByNtPerPost(Collection<AcePostLike> likes, String nt) {
        return likes.stream()
                .filter(like -> Objects.equals(like.getNt(), nt))
                .collect(Collectors.toMap(
                        AcePostLike::getPostId,
                        AcePostLike::getFlag));
    }

    @Override
    @Transactional
    public AceQuestionPost createQuestionPost(AceQuestionPost post) {
        AceQuestionPost post2Save = postRepository.findByIdAndPosterAndQuestionId(
                post.getId(), post.getPoster(), post.getQuestionId())
                .map(oldPost -> {
                    post.setReplyTo(null)
                            .setCreateTime(null)
                            .setUpdateTime(nowOfUtc())
                            .setAccepted(null);
                    BeanUtils.copyPropertiesIgnoreNull(post, oldPost);
                    return oldPost;
                })
                .orElseGet(() -> {
                            if (post.getReplyTo() != null &&
                                    !postRepository.existsByIdAndQuestionId(post.getReplyTo(), post.getQuestionId())) {
                                post.setReplyTo(null);
                            }
                            return post.setId(null)
                                    .setCreateTime(nowOfUtc())
                                    .setUpdateTime(nowOfUtc())
                                    .setAccepted(false);
                        }
                );


        return postRepository.save(post2Save);
    }

    @Override
    @Transactional
    public AceQuestionPost editQuestionPost(AceQuestionPost post, String editor) {
        AceQuestionPost post2Save = postRepository.findById(post.getId())
                .map(oldPost -> {
                    oldPost.setComment(post.getComment());
                    if (oldPost.getPoster().equals(editor)) {
                        oldPost.setUpdateTime(nowOfUtc());
                    } else {
                        oldPost.setEditor(editor);
                        oldPost.setEditTime(nowOfUtc());
                    }
                    return oldPost;
                })
                .orElseThrow(() -> new RuntimeException("Can not found post"));


        return postRepository.save(post2Save);
    }

    @Override
    public AceQuestionsDto listQuestions(ListQuestionOptions options) {
        Page<AceQuestion> questions;
        if (options.isPickUp()) {
            Pageable pageable = PageRequest.of(options.getPage(), options.getSize(), Sort.by("pickUp"));
            questions = questionRepository.findAllPickUp(pageable);
        } else if (options.getScope() == ListQuestionOptions.Scope.all) {
            Sort sort = Sort.by(options.getSortType().name()).descending();
            Pageable pageable = PageRequest.of(options.getPage(), options.getSize(), sort);
            questions = questionRepository.findAll(options.getStartDatetime(), pageable);
        } else {
            Sort sort = Sort.by(options.getSortType().name()).descending();
            Pageable pageable = PageRequest.of(options.getPage(), options.getSize(), sort);
            questions = questionRepository.findAll(options.getNt(), options.getStartDatetime(), pageable);
        }

        List<AceQuestionDto> questionDtoList = Collections.emptyList();
        if (questions.getNumberOfElements() > 0) {
            List<Integer> ids = questions.stream().map(AceQuestion::getId).collect(Collectors.toList());
            List<AceQuestionLike> questionLikes = questionLikeRepository.findAllByQuestionIdIn(ids);
            List<AcePostLike> postLikes = postLikeRepository.findAllByQuestionIdIn(ids);
            List<CountResult> postCountResult = postRepository.countPerQuestion(ids);
            List<CountResult> level1PostCountResult = postRepository.countLevel1PostPerQuestion(ids);

            Map<Integer, Integer> likedByNtPerQuestion = likeByNtPerQuestion(questionLikes, options.getNt());
            Map<Integer, Long> postLikePerQuestion = postLikePerQuestion(postLikes);
            Map<Integer, Long> likePerQuestion = likePerQuestion(questionLikes);
            Map<Integer, Long> countPerQuestion = postCountResult.stream()
                    .collect(Collectors.toMap(CountResult::getId, CountResult::getCount));
            Map<Integer, Long> countLevel1PostPerQuestion = level1PostCountResult.stream()
                    .collect(Collectors.toMap(CountResult::getId, CountResult::getCount));


            questionDtoList = questions.stream()
                    .map(q -> {
                        AceQuestionDto questionDto = new AceQuestionDto();
                        BeanUtils.copyProperties(q, questionDto);

                        questionDto.setTotalPost(countPerQuestion.getOrDefault(questionDto.getId(), 0L));
                        questionDto.setTotalLevel1Post(countLevel1PostPerQuestion.getOrDefault(questionDto.getId(), 0L));
                        questionDto.setTotalPostLike(postLikePerQuestion.getOrDefault(questionDto.getId(), 0L));
                        questionDto.setTotalLike(likePerQuestion.getOrDefault(questionDto.getId(), 0L));
                        questionDto.setLiked(likedByNtPerQuestion.getOrDefault(questionDto.getId(), 0));

                        return questionDto;
                    })
                    .collect(Collectors.toList());
        }

        return new AceQuestionsDto()
                .setQuestions(questionDtoList)
                .setSize(questionDtoList.size())
                .setTotalSize(questions.getTotalElements())
                .setTotalPage(questions.getTotalPages());
    }

    private Map<Integer, List<AceQuestionPostDto>> findPosts(Iterable<Integer> questionIds) {
        List<AceQuestionPost> posts = postRepository.findAllByQuestionIdIn(questionIds);
        if (posts.isEmpty()) {
            return Collections.emptyMap();
        }

        return posts.stream()
                .map(post -> {
                    AceQuestionPostDto postDto = new AceQuestionPostDto();
                    BeanUtils.copyProperties(post, postDto);
                    return postDto;
                })
                .collect(Collectors.groupingBy(AceQuestionPostDto::getQuestionId));
    }

    @Override
    @Transactional
    public AcePostLike votePost(String nt, Integer questionId, Integer postId, Integer flag) {
        Optional<AcePostLike> like = postLikeRepository.getOneByNtAndQuestionIdAndPostId(nt, questionId, postId);
        AcePostLike like2Save = like.orElse(fallbackAcePostLike(nt, questionId, postId, flag))
                .setFlag(flag)
                .setUpdateTime(nowOfUtc());
        return postLikeRepository.save(like2Save);
    }

    private AcePostLike fallbackAcePostLike(String nt, Integer questionId, Integer postId, Integer flag) {
        return new AcePostLike()
                .setNt(nt)
                .setQuestionId(questionId)
                .setPostId(postId)
                .setFlag(flag)
                .setCreateTime(nowOfUtc())
                .setUpdateTime(nowOfUtc())
                ;
    }

    @Override
    @Transactional
    public AceQuestionLike voteQuestion(String nt, Integer questionId, Integer flag) {
        Optional<AceQuestionLike> like = questionLikeRepository.getOneByNtAndQuestionId(nt, questionId);
        AceQuestionLike like2Save = like.orElse(fallbackAceQuestionLike(nt, questionId, flag))
                .setFlag(flag)
                .setUpdateTime(nowOfUtc());
        return questionLikeRepository.save(like2Save);
    }

    private AceQuestionLike fallbackAceQuestionLike(String nt, Integer questionId, Integer flag) {
        return new AceQuestionLike()
                .setNt(nt)
                .setQuestionId(questionId)
                .setFlag(flag)
                .setCreateTime(nowOfUtc())
                .setUpdateTime(nowOfUtc())
                ;
    }

    @Override
    @Transactional
    public Boolean deleteQuestion(Integer questionId, String nt) {
        if (questionRepository.existsByIdAndSubmitter(questionId, nt)) {
            deleteQuestionInternal(questionId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Boolean deletePost(Integer questionId, Integer postId, String nt) {
        if (postRepository.existsByIdAndQuestionIdAndPoster(postId, questionId, nt)) {
            deletePostInternal(postId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Boolean deleteQuestion(Integer questionId) {
        if (questionRepository.existsById(questionId)) {
            deleteQuestionInternal(questionId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Boolean deletePost(Integer questionId, Integer postId) {
        if (postRepository.existsById(postId)) {
            deletePostInternal(postId);
            return true;
        }
        return false;
    }

    private void deleteQuestionInternal(Integer questionId) {
        questionLikeRepository.deleteByQuestionId(questionId);
        postLikeRepository.deleteByQuestionId(questionId);
        postRepository.removeReplyTo(questionId);
        postRepository.deleteByQuestionId(questionId);
        questionRepository.deleteById(questionId);
    }

    private void deletePostInternal(Integer postId) {
        postLikeRepository.deleteByReplyTo(postId);
        postRepository.deleteByReplyTo(postId);
        postLikeRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public Boolean acceptPost(Integer questionId, Integer postId) {
        return postRepository.setAccepted(questionId, postId, true) > 0;
    }

    @Override
    @Transactional
    public Boolean revokeAcceptPost(Integer questionId, Integer postId) {
        return postRepository.setAccepted(questionId, postId, false) > 0;
    }

    @Override
    @Transactional
    public Boolean pickUpQuestion(Integer questionId, Integer score) {
        return questionRepository.setPickUp(questionId, score) > 0;
    }

    @Override
    @Transactional
    public Boolean revokePickUpQuestion(Integer questionId) {
        return questionRepository.setPickUp(questionId, 0) > 0;
    }
}

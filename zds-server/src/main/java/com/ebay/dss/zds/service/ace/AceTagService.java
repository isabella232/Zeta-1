package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.dao.ace.AceQuestionRepository;
import com.ebay.dss.zds.dao.ace.AceQuestionTagRepository;
import com.ebay.dss.zds.dao.ace.AceTagRepository;
import com.ebay.dss.zds.model.ace.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.common.DateUtil.nowOfUtc;

@Service
public class AceTagService {

    private final AceTagRepository tagRepository;
    private final AceQuestionTagRepository questionTagRepository;
    private final AceQuestionRepository questionRepository;

    public AceTagService(AceTagRepository tagRepository,
                         AceQuestionTagRepository questionTagRepository,
                         AceQuestionRepository questionRepository) {
        this.tagRepository = tagRepository;
        this.questionTagRepository = questionTagRepository;
        this.questionRepository = questionRepository;
    }

    @Cacheable(cacheNames = "aceHotTags")
    public List<AceTag> findHotTags(int size) {
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        List<Integer> ids = questionTagRepository
                .countHotTagIds(PageRequest.of(0, size))
                .stream()
                .map(AceQuestionCountByTag::getTagId)
                .collect(Collectors.toList());
        return tagRepository.findAllById(ids);
    }

    @Cacheable(cacheNames = "aceTags")
    public AceTagPage findTags(int page, int size, String nameStart) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createTime")));
        Page<AceTag> tagPage;
        if (StringUtils.isNotEmpty(nameStart)) {
            tagPage = tagRepository.findAllByNameStartingWith(nameStart, pageable);
        } else {
            tagPage = tagRepository.findAll(pageable);
        }
        return new AceTagPage()
                .setTags(tagPage.getContent())
                .setTotalPage(tagPage.getTotalPages())
                .setTotalSize(tagPage.getTotalElements());
    }

    public AceTag findOrCreateTag(AceTag tag) {
        return tagRepository.findByName(tag.getName())
                .orElseGet(() -> tagRepository.save(
                        tag.setCreateTime(nowOfUtc())
                                .setUpdateTime(nowOfUtc())));
    }

    public AceTag updateTag(AceTag tag) {
        return tagRepository.findById(tag.getId())
                .map(oldTag -> {
                    oldTag.setDescription(tag.getDescription());
                    oldTag.setName(tag.getName());
                    oldTag.setUpdateTime(nowOfUtc());
                    return tagRepository.save(oldTag);
                })
                .orElseThrow(() -> new RuntimeException("Tag not found"));
    }

    public AceTagQuestionsPage findQuestionsByAceTagIds(Set<Integer> tagIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("q.createTime")));
        Page<AceQuestionTag> questionTags = questionTagRepository
                .findByTagIdIn(tagIds, tagIds.size(), pageable);
        return new AceTagQuestionsPage()
                .setTagIds(tagIds)
                .setQuestionIds(questionTags.get().map(AceQuestionTag::getQuestionId).collect(Collectors.toList()))
                .setTotalPage(questionTags.getTotalPages())
                .setTotalSize(questionTags.getTotalElements());
    }

    public AceQuestionTagsMap findTagsByAceQuestionIds(Set<Integer> questionIds) {
        List<AceQuestionTag> questionTags = questionTagRepository.findByQuestionIdIn(questionIds);
        Set<Integer> tagIds = questionTags.stream()
                .map(AceQuestionTag::getTagId)
                .collect(Collectors.toSet());
        Map<Integer, AceTag> tagMap = tagRepository.findByIdIn(tagIds).stream()
                .collect(Collectors.toMap(AceTag::getId, Function.identity()));
        return questionTags.stream()
                .collect(Collectors.groupingBy(AceQuestionTag::getQuestionId,
                        AceQuestionTagsMap::new,
                        Collectors.mapping(qt -> tagMap.get(qt.getTagId()), Collectors.toSet())));

    }

    public AceQuestionTagsMap findTagsByAceQuestionId(Integer questionId) {
        return findTagsByAceQuestionIds(Collections.singleton(questionId));
    }

    @Transactional
    public void addTagsToQuestionId(Set<Integer> tagIds, Integer questionId, String nt) {
        if (questionRepository.existsByIdAndSubmitter(questionId, nt)) {
            questionTagRepository.saveAll(tagIds.stream()
                    .map(id -> new AceQuestionTag(questionId, id))
                    .collect(Collectors.toSet()));
        }
    }

    @Transactional
    public void addTagsToQuestionId(Set<Integer> tagIds, Integer questionId) {
        if (questionRepository.existsById(questionId)) {
            questionTagRepository.saveAll(tagIds.stream()
                    .map(id -> new AceQuestionTag(questionId, id))
                    .collect(Collectors.toSet()));
        }
    }

    @Transactional
    public void removeTagsFromQuestionId(Set<Integer> tagIds, Integer questionId, String nt) {
        if (questionRepository.existsByIdAndSubmitter(questionId, nt)) {
            questionTagRepository.deleteAllByQuestionIdAndTagIdIn(questionId, tagIds);
        }
    }

    @Transactional
    public void removeTagsFromQuestionId(Set<Integer> tagIds, Integer questionId) {
        if (questionRepository.existsById(questionId)) {
            questionTagRepository.deleteAllByQuestionIdAndTagIdIn(questionId, tagIds);
        }
    }

    public List<AceTag> findByTagIds(Set<Integer> tagIds) {
        return tagRepository.findByIdIn(tagIds);
    }
}

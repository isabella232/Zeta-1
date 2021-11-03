package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AceQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
public interface AceQuestionRepository extends JpaRepository<AceQuestion, Integer> {

    @Query(value = "select q from AceQuestion q where q.submitter = :nt and q.createTime >= :start")
    Page<AceQuestion> findAll(String nt, ZonedDateTime start, Pageable pageable);

    @Query(value = "select q from AceQuestion q where q.createTime >= :start")
    Page<AceQuestion> findAll(ZonedDateTime start, Pageable pageable);

    @Query(value = "select q from AceQuestion q where q.pickUp > 0")
    Page<AceQuestion> findAllPickUp(Pageable pageable);

    @Query(value = "select count(q) from AceQuestion q where q.submitter = :nt")
    Long count(String nt);

    Boolean existsByIdAndSubmitter(Integer id, String nt);

    Optional<AceQuestion> findByIdAndSubmitter(Integer id, String nt);

    @Modifying
    @Query(value="update AceQuestion q set q.pickUp = :score where q.id = :id")
    Integer setPickUp(Integer id, Integer score);
}

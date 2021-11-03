package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AceTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AceTagRepository extends JpaRepository<AceTag, Integer> {

    Page<AceTag> findAllByNameStartingWith(String nameStarting, Pageable pageable);

    Optional<AceTag> findByName(String name);

    List<AceTag> findByIdIn(Iterable<Integer> tagIds);
}

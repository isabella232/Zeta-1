package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZetaAttachmentRepository extends JpaRepository<ZetaAttachment, Long> {

    Optional<ZetaAttachment> findByFileName(String name);

    Page<ZetaAttachment> findAllByUploader(String nt, Pageable pageable);

    void deleteByFileNameAndUploader(String name, String uploader);
}

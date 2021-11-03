package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZetaEmailRepository extends JpaRepository<ZetaEmail, Long> {
}

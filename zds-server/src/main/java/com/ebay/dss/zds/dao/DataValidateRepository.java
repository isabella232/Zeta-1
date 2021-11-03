package com.ebay.dss.zds.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ebay.dss.zds.model.DataValidateDetail;
import org.springframework.stereotype.Repository;

/**
 * Created by zhouhuang on Apr 26, 2018
 */
@Repository
public interface DataValidateRepository extends JpaRepository<DataValidateDetail, Long> {

	List<DataValidateDetail> findByHistory_Status(int status);
}

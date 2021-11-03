package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Deprecated
public interface ZetaPackageRepository extends JpaRepository<ZetaPackage, Long> {
  List<ZetaPackage> findByNt(String nt);

  List<ZetaPackage> findByIdIn(List<Long> ids);

  List<ZetaPackage> findByFileNameAndNt(String fileName, String nt);

  List<ZetaPackage> findByFileNameAndNtAndCluster(String fileName, String nt, int cluster);

}

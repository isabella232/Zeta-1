package com.ebay.dss.zds.packages;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.packages.PackagesSubject.SubjectKey;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class NotebookPackagesObserver extends PackagesObserver {
  private final static Logger LOGGER = LoggerFactory.getLogger(NotebookPackagesObserver.class);


  @Autowired
  ZetaNotebookRepository zetaNotebookRepository;

  public NotebookPackagesObserver() {
    this.packagesSubject = PackagesSubject.packagesSubject;
    this.packagesSubject.attach(this);
  }

  public String getPackages(SubjectKey subjectKey) {
    PackagesSubject.PackageValue packageValue = packagesSubject.getPackages(subjectKey);
    Map<Integer, Set<String>> packageList = packageValue.getPackageList();
    Iterator<Map.Entry<Integer, Set<String>>> iter = packageList.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<Integer, Set<String>> entry = iter.next();
      Integer cluster = entry.getKey();
      Set<String> packages = entry.getValue();
      LOGGER.info("Append Cluster: {}", cluster);
      LOGGER.info("Append List: {}", packages);
      if (packageValue.isAppend) {
        String originPackages = zetaNotebookRepository.getNotebook(subjectKey.notebookId).getPackages();
        packageList = Objects.nonNull(originPackages) ? JsonUtil.fromJson(originPackages, new TypeReference<Map<Integer,
            Set<String>>>() {
        }) : packageList;
        packageList.get(cluster).addAll(packages);
      }
      LOGGER.info("Notebook {} Package List: {}", subjectKey.notebookId, packageList);
    }

    return JsonUtil.toJson(packageList);
  }


  @Override
  public void update(SubjectKey subjectKey) {
    LOGGER.info("Received package update message for {}", subjectKey);
    zetaNotebookRepository.updateNotebookPackagesByIdAndNt(subjectKey.notebookId, getPackages(subjectKey), subjectKey.userName);
  }
}

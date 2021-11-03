package com.ebay.dss.zds.packages;

import com.ebay.dss.zds.packages.PackagesSubject.SubjectKey;

public abstract class PackagesObserver {
  protected PackagesSubject packagesSubject;

  public abstract void update(SubjectKey subjectKey);
}

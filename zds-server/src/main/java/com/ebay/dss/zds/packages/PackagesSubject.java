package com.ebay.dss.zds.packages;

import com.ebay.dss.zds.common.Constant;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PackagesSubject {

  public static PackagesSubject packagesSubject;

  static {
    packagesSubject = new PackagesSubject();
  }

  private List<PackagesObserver> observerList = new ArrayList<>();

  private ConcurrentHashMap<SubjectKey, PackageValue> packageList = new ConcurrentHashMap<>();

  public PackageValue getPackages(SubjectKey subjectKey) {
    return packageList.get(subjectKey);
  }

  public void setPackages(SubjectKey subjectKey, PackageValue packages) {
    this.packageList.put(subjectKey, packages);
    notifyObservers(subjectKey);
  }

  public void attach(PackagesObserver packagesObserver) {
    observerList.add(packagesObserver);
  }

  public void notifyObservers(SubjectKey subjectKey) {
    observerList.stream().forEach(packagesObserver -> packagesObserver.update(subjectKey));
    packageList.remove(subjectKey);
  }

  public static class SubjectKey {
    @NotEmpty
    public final String userName;
    @NotEmpty
    public final String notebookId;

    private final String _str;

    public SubjectKey(String userName, String notebookId) {
      this.userName = userName;
      this.notebookId = notebookId;
      assert StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(notebookId);
      this._str = Constant.GLOBAL.workingUnitKey(userName, notebookId);
    }

    @Override
    public int hashCode() {
      return _str.hashCode();
    }

    @Override
    public boolean equals(Object anObject) {
      if (anObject instanceof SubjectKey) {
        return this.toString().equals(anObject.toString());
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return _str;
    }
  }

  public static class PackageValue {
    @JsonProperty("isAppend")
    boolean isAppend;
    Map<Integer, Set<String>> packageList;

    public boolean isAppend() {
      return isAppend;
    }

    public void setAppend(boolean isAppend) {
      this.isAppend = isAppend;
    }

    public Map<Integer, Set<String>> getPackageList() {
      return packageList;
    }

    public void setPackageList(Map<Integer, Set<String>> packageList) {
      this.packageList = packageList;
    }
  }
}

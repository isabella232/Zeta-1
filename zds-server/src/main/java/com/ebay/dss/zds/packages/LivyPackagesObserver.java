package com.ebay.dss.zds.packages;

import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage;
import com.ebay.dss.zds.interpreter.interpreters.livy.ZBaseLivyInterpreter;
import com.ebay.dss.zds.packages.PackagesSubject.SubjectKey;
import com.ebay.dss.zds.service.filestorage.FileType;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebay.dss.zds.service.filestorage.FileType.FILE;

@Component
public class LivyPackagesObserver extends PackagesObserver {

  private final static Logger LOGGER = LoggerFactory.getLogger(LivyPackagesObserver.class);

  @Autowired
  InterpreterManager interpreterManager;

  public LivyPackagesObserver() {
    this.packagesSubject = PackagesSubject.packagesSubject;
    this.packagesSubject.attach(this);
  }

  public Set<String> getPackages(SubjectKey subjectKey) {
    Map<Integer, Set<String>> packageList = packagesSubject.getPackages(subjectKey).getPackageList();
    for (Map.Entry<Integer, Set<String>> entry : packageList.entrySet()) {
      return entry.getValue();
    }
    return Sets.newHashSet();
  }

  @Override
  public void update(SubjectKey subjectKey) {
    String grpKey = interpreterManager.getFactory().getGrpKey(subjectKey);
    LOGGER.info("Received package update message for " + grpKey);
    InterpreterGroup grp = interpreterManager.getFactory().findInterpreterGroup(grpKey);
    if (grp != null) {
      Set<String> packageList = getPackages(subjectKey);
      for (Interpreter interpreter : grp.getInterpreters()) {
        if (interpreter instanceof ZBaseLivyInterpreter) {
          try {
            ZBaseLivyInterpreter baseLivyInterpreter = (ZBaseLivyInterpreter) interpreter;
            LivyClient livyClient = baseLivyInterpreter.getLivyClient();
            LivyMessage.SessionInfo sessionInfo = baseLivyInterpreter.getSessionInfo();
            requireAddResource(livyClient, packageList, sessionInfo);
          } catch (Exception ex) {
            LOGGER.error("Failed when updating package: " + ex.toString());
          }
        }
      }
    } else {
      LOGGER.info("Failed to update packages for " + grpKey + " because not group instance found");
    }
  }

  private static FileType getFileType(String path) {
    List<String> paths = Splitter.on(".").trimResults().splitToList(path);
    return paths.isEmpty() ? FILE : FileType.typeOf(paths.get(paths.size() - 1).toLowerCase());
  }

  public static void requireAddResource(LivyClient livyClient,
                                        Set<String> packageList,
                                        LivyMessage.SessionInfo sessionInfo) {
    if (sessionInfo != null) {
      for (String zetaPackage : packageList) {
        try {
          LivyMessage.AddResource addResource =
              new LivyMessage.AddResource(zetaPackage);
          switch (getFileType(zetaPackage)) {
            case JAR:
              livyClient.addJar(sessionInfo.id, addResource);
              break;
            case PY:
              livyClient.addPyFile(sessionInfo.id, addResource);
              break;
            case ZIP:
              livyClient.addPyFile(sessionInfo.id, addResource);
              break;
            case EGG:
              livyClient.addPyFile(sessionInfo.id, addResource);
              break;
            case FILE:
              livyClient.addFile(sessionInfo.id, addResource);
              // todo: add r
            default:
              livyClient.addFile(sessionInfo.id, addResource);
          }
          LOGGER.info("New package applied: " + zetaPackage);
        } catch (Exception ex) {
          LOGGER.error("Failed to apply package: " + zetaPackage);
        }
      }
    } else {
      LOGGER.error("No any SessionInfo provided! will not add any resource");
    }
  }
}

package com.ebay.dss.zds.interpreter;

import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup.StatusCode;
import com.ebay.dss.zds.state.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2018/5/18.
 */
@Component
public class InterpreterManager {

    private static final Logger logger = LoggerFactory.getLogger(InterpreterManager.class);

    private InterpreterFactory interpreterFactory;

    private StateManager stateManager;

    private ExecutorService executor;

    @Autowired
    public InterpreterManager(InterpreterFactory interpreterFactory,
                              StateManager stateManager,
                              InterpreterManagerExecutor executor) {
        this.interpreterFactory = interpreterFactory;
        this.stateManager = stateManager;
        this.executor = executor;
    }

    @PostConstruct
    private void recover() {
        this.interpreterFactory.recover(stateManager);
    }

    public static String genOnetimeNoteId(String nt, int platform) {
        return "onetime-" + nt + "-" + Optional.ofNullable(platform).orElse(999);
    }

    public static boolean isOnetimeNote(String noteId) {
        return Optional.ofNullable(noteId).orElse("").startsWith("onetime-");
    }

    public static boolean isOnetimeNote(InterpreterGroup intpGrp) {
        return Optional.ofNullable(intpGrp.getNoteId()).orElse("").startsWith("onetime-");
    }

    public String removeNotes(String userName) {
        List<Map.Entry<String, InterpreterGroup>> grps = interpreterFactory.searchInterpreterGroups(userName);
        if (grps == null || grps.size() == 0) {
            logger.info("Can't find any notes of user: {}", userName);
            return "Can't find any notes of user: " + userName;
        } else {
            grps.stream().forEach(intp -> {
                /**Don't close notes that is onetime note**/
                if (!isOnetimeNote(intp.getValue())) {
                    interpreterFactory.removeInterpreterGroup(intp.getKey(), false);
                }
            });
            logger.info("User: {}'s notes are all closed", userName);
            return "Cleaned: user = [" + userName + "] interpreter = [" + grps
                    .stream()
                    .map(kv -> kv.getKey())
                    .collect(Collectors.joining(", ")) + "]";
        }
    }

    public String removeContext(String userName) {
        /**remove rules first**/
        logger.info("Remove context of user: {}...", userName);
        String info = removeNotes(userName);
        removeRules(userName);
        logger.info("Remove context of user: {} done", userName);
        return info;
    }

    public void removeRules(String userName) {
        interpreterFactory.removeRules(userName);
    }

    public void closeNote(String userName, String noteId, String className) {
        interpreterFactory.closeInterpreter(userName, noteId, className);
    }

    public String removeNoteAsyncOrNot(String userName, String noteId, boolean async) {
        boolean flag = interpreterFactory.removeInterpreterGroup(interpreterFactory.getGrpKey(userName, noteId), false, async);
        if (flag) {
            logger.info("The note: id = [" + noteId + "] of user = [" + userName + "] has been closed");
        } else {
            logger.info("Failed to close note: id = [" + noteId + "] of user = [" + userName + "]");
        }
        String info = "The close command has been successfully submitted";
        logger.info(info);
        return info;
    }

    public String removeNote(String userName, String noteId) {
       return removeNoteAsyncOrNot(userName, noteId, true);
    }

    public String clearOnetimeNote(String userName) {
        List<InterpreterGroup> list = interpreterFactory.getAllInterpreterGrps()
                .values()
                .stream()
                .filter(intpGrp -> intpGrp.getUserName().equals(userName) && isOnetimeNote(intpGrp))
                .collect(Collectors.toList());

        list.forEach(intpGrp -> {
            if (interpreterFactory.removeInterpreterGroup(intpGrp.getGroupId(), false)) {
                logger.info("The onetime-note: id = [" + intpGrp.getNoteId() + "] of user = [" + intpGrp.getUserName() + "] has been closed");
            } else {
                logger.info("Failed to close the onetime-note: id = [" + intpGrp.getNoteId() + "] of user = [" + intpGrp.getUserName() + "]");
            }
        });
        String info = "The close command has been successfully submitted";
        logger.info(info);
        return info;
    }

    public boolean cleanFactory(boolean forceShutDown) {
        if (interpreterFactory != null) {
            return interpreterFactory.clear(forceShutDown);
        } else return true;
    }

    //todo:need to be elegant
    public String getAllInterpreterInfos() {
        String allInterpreterGroupList = interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .map(kv -> kv.getValue())
                .collect(Collectors.groupingBy(InterpreterGroup::getUserName))
                .entrySet().stream()
                .map(nt_list -> {
                    String interpreterGroups = nt_list.getValue()
                            .stream()
                            .map(InterpreterGroup::toJson)
                            .collect(Collectors.joining(","));
                    return "{\"userName\": \"" + nt_list.getKey() + "\", \"interpreterGroups\": [" + interpreterGroups + "]}";
                }).collect(Collectors.joining(","));

        return "{\"interpreterGroupList\": [" + allInterpreterGroupList + "]}";
    }

    public String getLifecycleInfos(String userName) {
        String interpreterGroups = interpreterFactory.getLifeCycleManager().getAllLifecycles().entrySet().stream()
                .filter(kv -> kv.getKey().getUserName().equals(userName))
                .map(kv -> kv.getKey().toJson()).collect(Collectors.joining(","));

        return "{\"userName\": \"" + userName + "\", \"interpreterGroups\": [" + interpreterGroups + "]}";
    }

    public String removeLifecycle(String userName, String noteId) {
        List<InterpreterGroup> intps = interpreterFactory.getLifeCycleManager().getAllLifecycles().entrySet().stream()
                .filter(kv -> kv.getKey().getUserName().equals(userName) && kv.getKey().getNoteId().equals(noteId))
                .map(kv -> kv.getKey())
                .collect(Collectors.toList());

        intps.forEach(i -> interpreterFactory.getLifeCycleManager().unregister(i));

        return intps.stream().map(InterpreterGroup::toJson).collect(Collectors.joining(","));
    }

    public String getAllLifecycleInfos() {
        String allInterpreterGroupList = interpreterFactory.getLifeCycleManager().getAllLifecycles().entrySet().stream()
                .map(kv -> kv.getKey())
                .collect(Collectors.groupingBy(InterpreterGroup::getUserName))
                .entrySet().stream()
                .map(nt_list -> {
                    String interpreterGroups = nt_list.getValue()
                            .stream()
                            .map(InterpreterGroup::toJson)
                            .collect(Collectors.joining(","));
                    return "{\"userName\": \"" + nt_list.getKey() + "\", \"interpreterGroups\": [" + interpreterGroups + "]}";
                }).collect(Collectors.joining(","));

        return "{\"interpreterGroupList\": [" + allInterpreterGroupList + "]}";
    }

    public String getInterpreterInfos(String userName) {
        String interpreterGroups = interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .filter(kv -> kv.getValue().getUserName().equals(userName))
                .map(kv -> kv.getValue().toJson()).collect(Collectors.joining(","));

        return "{\"userName\": \"" + userName + "\", \"interpreterGroups\": [" + interpreterGroups + "]}";
    }

    public long getAllInterpreterGroupCount() {
        return interpreterFactory.getAllInterpreterGrps().size();
    }

    // including dead one
    public long getCreatedInterpreterGroupCount() {
        return interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .filter(kv -> kv.getValue().getStatus() == InterpreterGroup.StatusCode.CREATED)
                .count();
    }

    public long getIdleInterpreterGroupCount() {
        return interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .filter(kv -> !kv.getValue().isBusy())
                .count();
    }

    public long getRunningInterpreterGroupCount() {
        return interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .filter(kv -> kv.getValue().getStatus() == InterpreterGroup.StatusCode.RUNNING)
                .count();
    }

    public long getConnectingInterpreterGroupCount() {
        return interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .filter(kv -> kv.getValue().getStatus() == InterpreterGroup.StatusCode.STARTING)
                .count();
    }

    public List<String> getConnectedNotebookIdByUserName(String userName) {
        return interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .map(kv -> kv.getValue())
                .filter(group ->
                        group.getUserName().equals(userName) &&
                                (group.isBusy() || group.getHealthReport().isHealth() || group.isAllOpened()))
                .map(group -> group.getNoteId())
                .collect(Collectors.toList());
    }

    @Deprecated
    public boolean isConnected(String noteId) {
        return interpreterFactory.getAllInterpreterGrps().entrySet().stream()
                .map(kv -> kv.getValue())
                .filter(group -> group.getNoteId().equals(noteId) && group.isAllOpened())
                .map(group -> group.getNoteId())
                .collect(Collectors.toList()).size() > 0;
    }

    public boolean isConnected(String noteId, String nt) {
        InterpreterGroup group = interpreterFactory.getAllInterpreterGrps().get(interpreterFactory.getGrpKey(nt, noteId));
        return group != null && group.isAllOpened();
    }

    public String cancelNote(String userName, String noteId) {
        logger.info("Handling cancel request from user: " + userName);
        InterpreterGroup intpGrp = interpreterFactory.searchInterpreterGroup(userName, noteId);
        List<Integer> stmtIds = new ArrayList<>();
        intpGrp.getInterpreters().forEach(intp ->
                stmtIds.addAll(interpreterFactory.cleanStatements(userName, noteId, intp.getClass()))
        );
        String cancelledInfo = "noteId: [" + noteId + "] cancelled" +
                (stmtIds.size() == 0 ?
                        " but no statements removed, because no interpreters found" :
                        " and removed statements: [" +
                                stmtIds
                                        .stream()
                                        .map(id -> id.toString())
                                        .collect(Collectors.joining(", ")) + "]");
        logger.info(cancelledInfo);
        logger.info("Cancel command executed");
        return cancelledInfo;
    }

    public String getUserFilterIntroduce(String userName) {
        return this.interpreterFactory.getLifeCycleManager().introduceUserRules(userName);
    }

    public InterpreterFactory getFactory() {
        return interpreterFactory;
    }

    @PreDestroy
    public void shutDown() {
        shutDown(false);
    }

    public void shutDown(boolean forceShutDown) {
        logger.info("Shut down the interpreter manager...");
        if (interpreterFactory != null) {
            interpreterFactory.shutDown(forceShutDown);
            interpreterFactory = null;
        } else logger.info("The interpreter manager has already been shut down");
        logger.info("Shut down the interpreter manager: done");
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public String grepConnectionStatus(String nt, String noteId) {
        InterpreterGroup interpreterGroup = interpreterFactory.searchInterpreterGroup(nt, noteId);
        if (interpreterGroup == null) {
            return StatusCode.mapToZetaStatus(StatusCode.NOT_EXISTS);
        } else {
            return StatusCode.mapToZetaStatus(interpreterGroup.getStatus());
        }
    }
}
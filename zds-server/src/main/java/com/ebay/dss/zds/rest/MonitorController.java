package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InvalidInputException;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.magic.DynamicVariableHandler;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.interpreter.api.InterpreterService;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import com.ebay.dss.zds.model.ZetaStatementContext;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.runner.ZetaExecutionPool;
import com.ebay.dss.zds.serverconfig.ServerConfigurationContext;
import com.ebay.dss.zds.service.BDPHTTPService;
import com.ebay.dss.zds.service.MonitorService;
import com.ebay.dss.zds.service.ZetaNotebookService;
import com.ebay.dss.zds.service.ZetaPackageService;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.websocket.SessionStoreService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by tatian on 2018/6/12.
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MonitorController.class);

    @Value("${zds.interpreter.api.enabled}")
    private boolean isEnabled;

    @Autowired
    private ZetaNotebookService zetaNotebookService;

    @Autowired
    private InterpreterService intpService;

    @Autowired
    private SessionStoreService sessService;

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private BDPHTTPService bdphttpService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ZetaPackageService zetaPackageService;

    @Autowired
    private ZetaExecutionPool zetaExecutionPool;

    @Autowired
    private StateManager stateManager;

    @Autowired
    private DynamicVariableHandler dynamicVariableHandler;

    @Autowired
    private ZetaStatementRepository zetaStatementRepository;

    @RequestMapping(value = "/info/interpreter", method = RequestMethod.GET)
    @ResponseBody
    public String interpreterInfos(@RequestParam(required = false) String userName) {
        if (isEnabled) {
            if(!StringUtils.isEmpty(userName)){
                return intpService.getInterpreterInfos(userName);
            }else {
                return intpService.getAllInterpreterInfos();
            }
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/info/lifecycle", method = RequestMethod.GET)
    @ResponseBody
    public String lifecycleInfos(@RequestParam(required = false) String userName) {
        if (isEnabled) {
            if(!StringUtils.isEmpty(userName)){
                return intpService.getLifecycleInfos(userName);
            }else {
                return intpService.getAllLifecycleInfos();
            }
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/info/filter", method = RequestMethod.GET)
    @ResponseBody
    public String filterInfos(@RequestParam(required = true) String userName) {
        if (isEnabled) {
            return intpService.getUserFilterIntroduce(userName);
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/remove/lifecycle/{userName}/{noteId}", method = RequestMethod.GET)
    @ResponseBody
    public String removeLifecycle(@PathVariable("userName") String userName,
                                  @PathVariable("noteId") String noteId) {
        if (isEnabled) {
            if(!StringUtils.isEmpty(userName)){
                return "Please provide userName";
            }else {
                return intpService.removeLifecycle(userName, noteId);
            }
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/info/user", method = RequestMethod.GET)
    @ResponseBody
    public String userInfos() {
        if (isEnabled) {
            return sessService.userInfos();
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/info/user_queue_access/{clusterName}/{userName}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse userQueueAccessInfos(@PathVariable("clusterName") String clusterName,
                                             @PathVariable("userName") String userName) {
        if (clusterName == null || userName == null) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Please provide clusterName and userName");
        }

        List<String> queues = bdphttpService.getUserQueueInfo(clusterName, userName);
        if (queues != null) {
            return new ZetaResponse<>(queues, HttpStatus.OK);
        } else {
            throw new EntityNotFoundException("Fail to get user accessible queues");
        }
    }

    @RequestMapping(value = "/info/user_cluster_access/{userName}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse userClusterAccessInfos(@PathVariable("userName") String userName) {
        if (userName == null) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Please provide userName");
        }

        List<String> clusters = bdphttpService.getUserClusterInfo(userName);
        if (clusters != null) {
            return new ZetaResponse<>(clusters, HttpStatus.OK);
        } else {
            throw new EntityNotFoundException("Fail to get user accessible clusters");
        }
    }

    @RequestMapping(value = "/status/queue/{clusterId}/{queueName}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse clusterStatus(@PathVariable("clusterId") Integer clusterId,
                                      @PathVariable("queueName") String queueName) {
        if(clusterId==null){
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Please provide clusterId");
        }
        try {
            List<YarnQueue> queues = monitorService.fetchQueues(clusterId, queueName);
            return new ZetaResponse(queues,HttpStatus.OK);
        }catch (RuntimeException ex){
            LOGGER.debug("fetch status failed cluster={}, queue={}, {}", clusterId, queueName, ex);
            return ZetaResponse.notFoundResponse();
        }
    }

    @RequestMapping(value = "/info/user_batch_access/{clusterName}/{userName}", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getBatchAccount(@PathVariable("clusterName") String clusterName,
                                        @PathVariable("userName") String userName) {
        List batchInfoList = bdphttpService.getUserBatchInfo(clusterName, userName);
        if (Objects.nonNull(batchInfoList)) {
            return batchInfoList;
        } else {
            throw new EntityNotFoundException("Fail to get user batch info");
        }
    }

    @RequestMapping(value = "/status/queue/notebookId/{noteId}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse clusterStatus(@PathVariable("noteId") String noteId,
                                      @RequestParam(required = false) Integer clusterId,
                                      @RequestParam(required = false) String nt) {
        if (noteId == null) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Please provide noteId");
        }

        if (StringUtils.isEmpty(nt)) {
            LOGGER.warn("The nt is not provided, maybe search in DB to get the nt");
        }

        try {
            List<YarnQueue> queues = clusterId == null ?
                    monitorService.fetchQueues(noteId, nt) :
                    monitorService.fetchQueues(noteId, clusterId, nt);
            return new ZetaResponse(queues,HttpStatus.OK);
        }catch (Exception ex){
            LOGGER.debug("fetch status failed, notebook={}, cluster={}, nt={}, {}", noteId, clusterId, nt, ex);
            return ZetaResponse.notFoundResponse();
        }
    }

    @RequestMapping(value = "/kick/{user}", method = RequestMethod.GET)
    @ResponseBody
    public String kickUser(@PathVariable("user") String user, @RequestParam(required = false) String noteId) {
        if (isEnabled) {
            return intpService.kickUser(user, noteId);
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/kick-onetime/{user}", method = RequestMethod.GET)
    @ResponseBody
    public String kickOnetimeNote(@PathVariable("user") String user) {
        if (isEnabled) {
            return intpService.kickOnetimeNote(user);
        } else return "The interpreter api is not enabled!";
    }

    @RequestMapping(value = "/connqueue/{clusterId}/{action}", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ZetaResponse toggleConnQueue(@PathVariable("clusterId") Integer clusterId,
                                        @PathVariable("action") String action) {
        if (clusterId == null) {
            return new ZetaResponse<>("Please provide clusterId", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (action == null) {
            return new ZetaResponse<>("Please provide action (on/off)", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (action.equals("on")) {
            InterpreterConfiguration.setUseConnQueue(clusterId, true);
            LOGGER.info(String.format("Connection queue of cluster %s is set to on", clusterId));
        } else {
            InterpreterConfiguration.setUseConnQueue(clusterId, false);
            LOGGER.info(String.format("Connection queue of cluster %s is set to off", clusterId));
        }

        return new ZetaResponse<>("OK", HttpStatus.OK);
    }

    @RequestMapping(value = "/connqueue/status", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse connQueueStatus() {
        Map<Integer, Boolean> useConnQueue = InterpreterConfiguration.getUseConnQueue();
        return new ZetaResponse<>(useConnQueue.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/taskexecutor/info", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse webSocketOutBoundQueueSize() {
        ThreadPoolExecutor inboundExecutor = ServerConfigurationContext
                .webSocketInboundExecutor
                .getThreadPoolExecutor();
        ThreadPoolExecutor outboundExecutor = ServerConfigurationContext
                .webSocketOutboundExecutor
                .getThreadPoolExecutor();
        ThreadPoolExecutor brokerExecutor = ServerConfigurationContext
                .webSocketMessageBrokerExecutor
                .getThreadPoolExecutor();
        Map<String, String> map = new HashMap<>();
        map.put("InBoundChannelExecutor", inboundExecutor.toString());
        map.put("OutBoundChannelExecutor", outboundExecutor.toString());
        map.put("MessageBrokerExecutor", brokerExecutor.toString());
        map.putAll(this.zetaExecutionPool.showTenants());
        return new ZetaResponse<>(map, HttpStatus.OK);
    }

    @RequestMapping(value = "/configuration/reload", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse reloadConfiguration() {

        InterpreterConfiguration conf = configurationManager.reloadConfiguration();

        Map<String, String> map = new HashMap<>();

        map.put("new_config", conf.getProperties().toString());

        return new ZetaResponse<>(map, HttpStatus.OK);
    }

    @RequestMapping(value = "/dynamic_monitor/{action}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse toggleDynamicMonitor(@PathVariable("action") String action) {

        if (action.equals("on")) {
            monitorService.toggleDynamicMonitor(true);
            LOGGER.info("Dynamic monitor is set to on");
        } else {
            monitorService.toggleDynamicMonitor(false);
            LOGGER.info("Dynamic monitor is set to off");
        }

        return new ZetaResponse<>("OK", HttpStatus.OK);
    }

    @RequestMapping(value = "/package/list", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> listPackages(@RequestParam("nt") String nt,
                                            @RequestParam("clusterId") Integer clusterId,
                                            @RequestParam("path") String path) {
        return zetaPackageService.getHDFSFolderList(nt, clusterId, path);
    }

    @RequestMapping(value = "/status/note/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse getNoteStatus(@PathVariable("id") String id,
                                      @RequestParam(required = true) String nt) {
        return new ZetaResponse<>(zetaNotebookService.getCurrentNoteRunningStatus(nt, id), HttpStatus.OK);
    }

    @RequestMapping(value = "/execution_pool/{action}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse toggleExecutionPool(@PathVariable("action") String action) {

        if (action.equals("on")) {
            zetaExecutionPool.enable();
            LOGGER.info("zetaExecutionPool is set to on");
        } else {
            zetaExecutionPool.disable();
            LOGGER.info("zetaExecutionPool is set to off");
        }

        return new ZetaResponse<>("OK", HttpStatus.OK);
    }

    @RequestMapping(value = "/state_manager/{action}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse toggleStateManager(@PathVariable("action") String action) {

        if (action.equals("on")) {
            stateManager.enable();
            LOGGER.info("stateManager is set to on");
        } else {
            stateManager.disable();
            LOGGER.info("stateManager is set to off");
        }

        return new ZetaResponse<>("OK", HttpStatus.OK);
    }

    @RequestMapping(value = "/status/clear/note/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse clearNoteStatus(@PathVariable("id") String id,
                                        @RequestParam(required = false) Long statementId) {
        if (statementId != null) {
            zetaNotebookService.cleanCurrentNoteStatus(id, statementId);
            return new ZetaResponse<>(String.format("Note: %s statement: %s status cleared", id, statementId), HttpStatus.OK);
        } else {
            zetaNotebookService.cleanCurrentNoteStatus(id);
            return new ZetaResponse<>(String.format("Note: %s status cleared", id), HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/dynamic_variable/{action}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse toggleDynamicVariable(@PathVariable("action") String action) {

        if (action.equals("on")) {
            dynamicVariableHandler.setEnabled(true);
            LOGGER.info("Dynamic variable is set to on");
        } else {
            dynamicVariableHandler.setEnabled(false);
            LOGGER.info("Dynamic variable is set to off");
        }

        return new ZetaResponse<>("OK", HttpStatus.OK);
    }

    @RequestMapping(value = "/context/statement/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ZetaResponse getStatementContext(@PathVariable("id") Long id) {

        ZetaStatementContext context = zetaStatementRepository.getZetaStatementContext(id);
        if (context != null) {
            return new ZetaResponse<>(context.toJson(), HttpStatus.OK);
        } else {
            return new ZetaResponse<>("Can't find any statement context with id: " + id, HttpStatus.BAD_REQUEST);
        }
    }
}

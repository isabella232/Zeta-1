package com.ebay.dss.zds.websocket;

import com.ebay.dss.zds.interpreter.InterpreterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2018/6/6.
 */
@Service
public class SessionStoreService {

    private final static Logger logger = LoggerFactory.getLogger(SessionStoreService.class);

    private static final ConcurrentHashMap<String, String> lastSessionMap = new ConcurrentHashMap<>();

    private InterpreterManager manager;

    @Autowired
    public SessionStoreService(InterpreterManager manager) {
        this.manager = manager;
    }

    public void store(String nt, String sessionKey) {
        lastSessionMap.put(nt, sessionKey);
        logger.info("Stored session: user = [{}], sessionId = [{}]", nt, sessionKey);
    }

    @Deprecated
    public void replaceAndStore(String nt,String sessionKey){
        if(lastSessionMap.containsKey(nt)){
            String psk=lastSessionMap.get(nt);
            logger.info("New Connection from: user = [{}], sessionId = [{}], remove previous session = [{}]...", nt, sessionKey,psk);
            logger.info("User = [{}] : Close all notes...", nt);
            manager.removeContext(nt);
            logger.info("User = [{}] : Notes of previous session are closed, new session is ready to store", nt);
        }
        store(nt,sessionKey);
    }

    public String getSessionKey(String nt) {
        return lastSessionMap.get(nt);
    }

    public boolean checkClosed(String nt, String sessionKey) {
        return lastSessionMap.getOrDefault(nt, "").equals(sessionKey);
    }

    public void removeSession(String nt) {
        logger.info("Removed session: user = [{}], sessionId = [{}]", nt, lastSessionMap.remove(nt));
    }

    public void offline(Principal principal) {
        logger.info("User: {} is offline, close all notes...", principal);
        String info = manager.removeContext(principal.getName());
        removeSession(principal.getName());
        logger.info("Close command submitted: \n" + info);
    }

    public String userInfos(){
        int activeUserCnt=lastSessionMap.size();
        return "{\"User Count\": "+activeUserCnt+", \"User List\":["+
                lastSessionMap.entrySet().stream().map(
                        kv->
                            "{\"UserName\": \""+kv.getKey()+"\", \"SessionKey\": \""+kv.getValue()+"\"}"
                ).collect(Collectors.joining(","))
                +"]}";
    }

}

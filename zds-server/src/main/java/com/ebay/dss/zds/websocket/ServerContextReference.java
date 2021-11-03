package com.ebay.dss.zds.websocket;

import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.serverconfig.ApplicationContextProvider;
import com.ebay.dss.zds.websocket.notebook.CodeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ServerContextReference {
    private static final Logger logger = LoggerFactory.getLogger(ServerContextReference.class);

    private static final ConcurrentHashMap<Class,Object> registeredReference=new ConcurrentHashMap<>();

    public static <T> T register(T obj){
        registeredReference.put(obj.getClass(),obj);
        logger.info("Registered class: {}",obj.getClass().getName());
        return obj;
    }

    public static Object getRegistered(Class claz){
        Object obj = registeredReference.get(claz);
        if (obj == null) {
            Object bean = null;
            try {
                bean = ApplicationContextProvider.getBean(claz);
            } catch (Exception ex) {
                logger.error(ex.toString());
            }
            if (bean != null) register(bean);
            return bean;
        } else return obj;
    }

    public static void sendSocketRspToUser(String userName,WebSocketResp wsr){
        /**send the message to user endpoint**/
        try {
            CodeController cc = (CodeController) ServerContextReference.getRegistered(CodeController.class);
            QueueDestination dest = new QueueDestination(userName, cc.getTemplate(), cc.getQueue());
            dest.sendData(wsr);
        }catch (Exception ex){
            logger.error("Failed to send disconnect info to user: {} by: {}",userName,ex.toString());
        }
    }


}

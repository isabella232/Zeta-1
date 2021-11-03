package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleManager;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleModel;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.ebay.dss.zds.message.event.ZetaLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ebay.dss.zds.interpreter.lifecycle.LifeCycleManager.longCheckTimeOutKey;

/**
 * Created by tatian on 2019-06-21.
 */
public class LifecycleEventSink implements ZetaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleEventSink.class);

    private LifeCycleManager lifeCycleManager;

    private long longCheckingTimeOut;

    public LifecycleEventSink(LifeCycleManager lifeCycleManager) {
        this.lifeCycleManager = lifeCycleManager;
        this.longCheckingTimeOut = Long.valueOf(lifeCycleManager
                .getProp()
                .getProperty(longCheckTimeOutKey, "10000"));
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
        if (zetaEvent instanceof ZetaLifecycleEvent) {
            LifeCycleModel cycle = ((ZetaLifecycleEvent) zetaEvent).getLifeCycle();

            if (cycle == null) return;

            InterpreterGroup intp = (InterpreterGroup) cycle.getCompanionObject();
            logger.debug("Check lifecycle of interpreter group: {}", intp.getGroupId());
            boolean isAlive=true;

            long aliveCheckStart = System.currentTimeMillis();
            try {
                isAlive = cycle.isAlive();
            }catch (Exception ex){
                logger.error("Failed to check lifecycle of group: {} ,cause: {}",intp.getGroupId(),ex.toString());
                isAlive = false;
            }
            long aliveCheckCost = System.currentTimeMillis() - aliveCheckStart;
            if (aliveCheckCost >= longCheckingTimeOut) {
                logger.warn("Long checking, alive check: [cost: {}, groupId: {}]", aliveCheckCost, intp.getGroupId());
            }

            if (!isAlive) {
                logger.info("Detected interpreter group: {} is inactive, close it...  reason: {}", intp.getGroupId(),cycle.endReason());
                long endAndCloseStart = System.currentTimeMillis();
                try {
                    cycle.tryEnd();
                } catch (Exception ex) {
                    logger.error("Got ex when end the lifecycle: "+ intp.getGroupId());
                }
                lifeCycleManager.unregister(intp);
                long endAndCloseCost = System.currentTimeMillis() - endAndCloseStart;
                if (endAndCloseCost >= longCheckingTimeOut) {
                    logger.warn("Long checking, end and close check: [cost: {}, groupId: {}]", endAndCloseCost, intp.getGroupId());
                }

                logger.info("Closed: {}", intp.getGroupId());
            }else{
                logger.debug(intp.getGroupId()+" is alive, do cleaning");
                long gcStart = System.currentTimeMillis();
                try {
                    intp.doGc();
                }catch (Exception ex){
                    logger.error(ex.toString());
                }
                long gcCost = System.currentTimeMillis() - gcStart;
                if (gcCost >= longCheckingTimeOut) {
                    logger.warn("Long checking, gc check: [cost: {}, groupId: {}]", gcCost, intp.getGroupId());
                }
                logger.debug(intp.getGroupId()+" cleaning done");
            }
            logger.debug("Check done: {}", intp.getGroupId());
        }
    }
}

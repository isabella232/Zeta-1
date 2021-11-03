package com.ebay.dss.zds.interpreter.lifecycle;


import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.websocket.ServerContextReference;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.SessionExpiredMsg;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tatian on 2018/5/28.
 */
public class TimeoutModel extends AbstractLifeCycleModel {


    private InterpreterGroup intpGrp;

    private volatile DateTime last;

    private final int timeout;

    public static final String default_timeout = "18000000";

    public static final String TIME_OUT = "zds.interpreter.lifecycle.model.timeout.interval.millis";

    private volatile boolean isDead = false;

    private volatile boolean isTimeout = false;

    private String reason="The session is timeout";

    private HealthReport healthReport = new HealthReport(false);

    public TimeoutModel(LifeCycleManager manager, Properties prop) {
        super(manager, prop);
        this.timeout = Integer.valueOf(prop.getProperty(TIME_OUT, default_timeout));
    }

    private TimeoutModel(LifeCycleManager manager, int timeout) {
        super(manager);
        this.timeout = timeout;
    }

    public LifeCycleModel create() {
        return new TimeoutModel(this.manager, Integer.valueOf(prop.getProperty(TIME_OUT, default_timeout)));
    }

    public LifeCycleModel bind(Object obj) {
        this.intpGrp = (InterpreterGroup) obj;
        this.last = new DateTime();
        return this;
    }

    public Object getCompanionObject() {
        return this.intpGrp;
    }

    public boolean isAlive() {
        boolean isAlive;
        if (last == null) {
            isAlive = false;
        } else {
            try {
                isDead = !intpGrp.hasOpened();
                // pay attention: the .getMillis() may throw: java.lang.ArithmeticException: Value cannot fit in an int: 2843846633
                isTimeout = !(new Period(last, new DateTime(), PeriodType.millis()).getMillis() <= timeout);
                isAlive = intpGrp.isBusy() || (!isDead && !isTimeout);
                if (!isAlive)
                    reason = isDead ? "The session is dead" : (isTimeout ? "The session is timeout" : "Unknown reason");
            } catch (Exception ex) {
                reason = "Exception when check lifecycle: " + ex.toString();
                isAlive = false;
            }
        }

        if (isAlive) {
            this.healthReport.setHealth(true);
        } else {
            this.healthReport.setHealth(false);
        }

        return isAlive;
    }

    public void keepAlive() {
        this.last = last.withTime(LocalTime.now());
    }

    public void end() {
        if (intpGrp != null) {
            try {
                logger.debug("Interpreter Group: {} is at the end of lifecycle...", intpGrp.getGroupId());
                manager.getInterpreterFactory().removeInterpreterGroup(intpGrp.getGroupId(), true);
                //manager.unregister(intpGrp); //already unregister in manager
                logger.debug("Interpreter Group: {} is ended", intpGrp.getGroupId());

                /**send the message to user endpoint**/
                ServerContextReference.sendSocketRspToUser(intpGrp.getUserName(),
                        new WebSocketResp(WebSocketResp.OP.NB_CODE_SESSION_EXPIRED, new SessionExpiredMsg(intpGrp.getNoteId(), reason)));
            } finally {
                this.healthReport.setHealth(false);
            }
        }
    }

    public String endReason(){
        return reason;
    }

    public HealthReport getHealthReport() {
        return this.healthReport;
    }

}

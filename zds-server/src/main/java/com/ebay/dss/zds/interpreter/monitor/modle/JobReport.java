package com.ebay.dss.zds.interpreter.monitor.modle;

import java.util.List;
import java.util.Map;

public class JobReport implements Status {
    public int statementId;
    public List<Integer> activeJobIds;
    public String status;
    public int totalNumTasks;
    public int totalNumStages;
    public int totalNumCompletedTasks;
    public int totalNumFailedTasks;
    public int totalNumCompletedStages;
    public int numActiveTasks;
    public int numCompletedCurrentStageTasks;
    public int numTotalCurrentStageTasks;
    public int numActiveStages;
    public List<Integer> activeStageIds;
    public Map<String, String> externalContext;
    public String dag;
    public double progress;

    public StatusEnum getStatus(){ return StatusEnum.fromString(status);}
    public JobReport setStatus(StatusEnum status){
        this.status=status.getName();
        return this;
    }
    public JobReport setProgress(double progress){
        this.progress=progress;
        return this;
    }
    public double getProgress(){return this.progress;}
}

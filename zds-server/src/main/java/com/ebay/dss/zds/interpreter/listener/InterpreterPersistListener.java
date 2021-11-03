package com.ebay.dss.zds.interpreter.listener;

import com.ebay.dss.zds.dao.ZetaJobRequestRepository;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.output.ResultEnum;
import com.ebay.dss.zds.interpreter.output.result.Result;
import com.ebay.dss.zds.model.ZetaJobRequest;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaStatement;
import com.ebay.dss.zds.model.ZetaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

/**
 * Created by wenliu2 on 5/7/18.
 */
public class InterpreterPersistListener extends InterpreterZetaBaseListener {

    private static final Logger logger = LoggerFactory.getLogger(InterpreterPersistListener.class);
    private ZetaStatementRepository zetaStatementRepository;
    private ZetaJobRequestRepository zetaJobRequestRepository;
    private ZetaNotebookRepository zetaNotebookRepository;
    private Long zetaStatementKey;

    public InterpreterPersistListener(ZetaNotebook notebook,
                                      Interpreter interpreter,
                                      ZetaStatementRepository zetaStatementRepository,
                                      ZetaJobRequestRepository zetaJobRequestRepository,
                                      ZetaNotebookRepository zetaNotebookRepository) {
        super(notebook, interpreter);
        this.zetaStatementRepository = zetaStatementRepository;
        this.zetaJobRequestRepository = zetaJobRequestRepository;
        this.zetaNotebookRepository = zetaNotebookRepository;
    }

    public ZetaStatementRepository getZetaStatementRepository() {
        return zetaStatementRepository;
    }

    public ZetaJobRequestRepository getZetaJobRequestRepository() {
        return zetaJobRequestRepository;
    }

    public ZetaNotebookRepository getZetaNotebookRepository() {
        return zetaNotebookRepository;
    }

    @Override
    public void doBeforeStatementSubmit(InterpreterListenerData data) {
        logger.info("Created: Notebook[id=" + notebook.getId() + "]" + " - JobRequest[id=" + this.getJobId() + "]");
    }

    @Override
    Long doAfterStatementSubmit(InterpreterListenerData data) {

        ZetaStatement zetaStatement = new ZetaStatement();
        zetaStatement.setProgress(0);
        zetaStatement.setLivyStatementId(data.getStatementId());
        zetaStatement.setLivySessionId(data.getSessionId());
        zetaStatement.setStatus(ZetaStatus.SUBMITTED.getStatusCode());
        zetaStatement.setRequestId(Long.valueOf(this.getJobId()));
        zetaStatement.setSeq(this.getCodeWithSeq().getSeq());
        zetaStatement.setStatement(this.getCodeWithSeq().getCode());
        zetaStatement.setStartDt(new Timestamp(startDt.getMillis()));
        this.zetaStatementKey = zetaStatementRepository.addZetaStatement(zetaStatement);

        ZetaJobRequest jobRequest = zetaJobRequestRepository.getZetaJobRequest(Integer.valueOf(jobId));
        jobRequest.setLivyJobUrl(data.getSparkJobUrl());
        zetaJobRequestRepository.updateZetaJobRequest(jobRequest);

        notebook.setLastRunDt(new Timestamp(startDt.getMillis()));
        zetaNotebookRepository.refreshLastDt(notebook);
        logger.info("Submitted: Notebook[id=" + notebook.getId() + "]" + " - JobRequest[id=" + this.getJobId() +
                "] - " + "Statement[id=" + data.getStatementId() + "]");
        return zetaStatementKey;
    }

    @Override
    void doAfterStatementFinish(InterpreterListenerData data, Result result) {
        String statusCode = result.getResultCode()
                .equals(ResultEnum.ResultCode.ERROR) ?
                ZetaStatus.FAIL.getStatusCode() :
                ZetaStatus.FINISHED.getStatusCode();
        try {
            zetaStatementRepository.updateUnfinishedZetaStatementStateById(
                    this.zetaStatementKey, statusCode, result.get(), 1, new Timestamp(endDt.getMillis()));
        } catch (Exception e) {
            logger.warn("Update result of statement failed, maybe result is too large, update a empty result instead");
            try {
                zetaStatementRepository.updateUnfinishedZetaStatementStateById(
                        this.zetaStatementKey, statusCode, "", 1, new Timestamp(endDt.getMillis()));
            } catch (Exception e2) {
                logger.error("Update result of statement failed again, ", e2);
            }
        }
        logger.info("Finished: Notebook[id=" + notebook.getId() + "]" + " - JobRequest[id=" + this.getJobId() +
                "] - " + "Statement[id=" + data.getStatementId() + "]");
    }

    @Override
    void doHandleCancelling(InterpreterListenerData data) {
        try {
            zetaStatementRepository.updateUnfinishedZetaStatementStateById(
                    this.zetaStatementKey, ZetaStatus.CANCELED.getStatusCode(),
                    "", 0, new Timestamp(endDt.getMillis()));
            logger.info("Canceled: Notebook[id=" + notebook.getId() + "]" + " - JobRequest[id=" + this.getJobId() + "]");
        } catch (Exception ex) {
            logger.error("Exception happened during update state: " + ex.getMessage());
        }
    }

    @Override
    public long getCurrentStatementKey() {
        return this.zetaStatementKey;
    }
}

package com.ebay.dss.zds.interpreter.listener;

import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.dao.ZetaJobRequestRepository;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InterpreterListenerFactory {

    private ZetaJobRequestRepository zetaJobRequestRepository;
    private ZetaStatementRepository zetaStatementRepository;
    private ZetaNotebookRepository zetaNotebookRepository;

    public InterpreterListenerFactory() {
    }

    @Autowired
    public InterpreterListenerFactory(ZetaJobRequestRepository zetaJobRequestRepository,
                                      ZetaStatementRepository zetaStatementRepository,
                                      ZetaNotebookRepository zetaNotebookRepository) {
        this.zetaJobRequestRepository = zetaJobRequestRepository;
        this.zetaStatementRepository = zetaStatementRepository;
        this.zetaNotebookRepository = zetaNotebookRepository;
    }

    public InterpreterListener getInstance(
            ZetaNotebook notebook,
            Interpreter interpreter,
            String jobId,
            CodeWithSeq codeWithSeq,
            ExecuteCodeReq req,
            QueueDestination dest) {

        InterpreterPersistListener listener = new InterpreterPersistListener(
                notebook,
                interpreter,
                zetaStatementRepository,
                zetaJobRequestRepository,
                zetaNotebookRepository);
        listener.setJobId(jobId);
        listener.setCodeWithSeq(codeWithSeq);
        listener.setReqId(req.getReqId());
        listener.setDestination(dest);
        listener.setListenerType(req);

        return new InterpreterTrackListenerProxy(listener);
    }
}

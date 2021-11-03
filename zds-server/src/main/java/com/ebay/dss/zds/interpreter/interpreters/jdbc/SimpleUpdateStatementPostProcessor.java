package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zeppelin.interpreter.InterpreterResult;

import java.sql.SQLException;
import java.sql.Statement;

class SimpleUpdateStatementPostProcessor implements StatementPostProcessor<InterpreterResult> {

    private static final Logger logger = LogManager.getLogger();

    static InterpreterResult prepareUpdateResult(Statement ps) throws SQLException {
        logger.info("current statement return an update result, building...");
        int updateCount = ps.getUpdateCount();
        return new InterpreterResultBuilder().success()
                .table()
                .setCount(updateCount)
                .next()
                .build();
    }

    @Override
    public InterpreterResult execute(Statement statement) throws Exception {
        return prepareUpdateResult(statement);
    }

}

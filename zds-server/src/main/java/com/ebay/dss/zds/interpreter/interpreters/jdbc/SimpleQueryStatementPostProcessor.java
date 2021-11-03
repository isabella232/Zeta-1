package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zeppelin.interpreter.InterpreterResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class SimpleQueryStatementPostProcessor implements StatementPostProcessor<InterpreterResult> {

    private static final Logger logger = LogManager.getLogger();
    private JdbcCompleter completer;

    public SimpleQueryStatementPostProcessor(JdbcCompleter completer) {
        this.completer = completer;
    }

    static InterpreterResult prepareQueryResult(Statement ps, int rowMax) throws SQLException {
        logger.info("current statement return an query result, building...");
        List<String> labels;
        List<List<Object>> rows;
        long updateCount = 0;
        try {
            updateCount = ps.getUpdateCount();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try (ResultSet rs = ps.getResultSet()) {
            ResultSetMetaData metaData = rs.getMetaData();
            labels = prepareColumnLabels(metaData);
            rows = prepareRows(rs, labels, rowMax);
        }
        return new InterpreterResultBuilder().success()
                .table(labels, rows, rows.size(), updateCount)
                .build();
    }

    static List<String> prepareColumnLabels(ResultSetMetaData metaData) throws SQLException {
        int columnCount = metaData.getColumnCount();
        List<String> columnLabels = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columnLabels.add(metaData.getColumnLabel(i));
        }
        return columnLabels;
    }

    static List<List<Object>> prepareRows(ResultSet rs,
                                          List<String> labels,
                                          int rowMax) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        int rowCnt = 0;
        while (rs.next()) {
            rowCnt++;
            List<Object> row = new ArrayList<>();
            IntStream.rangeClosed(1, labels.size())
                    .forEachOrdered(idx -> {
                        try {
                            row.add(rs.getObject(idx));
                        } catch (SQLException e) {
                            logger.error("fetch cell error", e);
                        }
                    });
            if (row.size() == labels.size()) {
                rows.add(row);
            }
            if (rowCnt >= rowMax) {
                break;
            }
        }
        return rows;
    }

    @Override
    public InterpreterResult execute(Statement statement) throws SQLException {
        int rowMax = completer.getConf().getRowMax();
        return prepareQueryResult(statement, rowMax);
    }
}

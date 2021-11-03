package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.common.TempFile;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.opencsv.CSVWriter;
import org.apache.zeppelin.interpreter.InterpreterResult;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.ebay.dss.zds.common.Constant.*;

class DumpStatementPostProcessor implements StatementPostProcessor<InterpreterResult> {

    private JdbcCompleter completer;
    private ExecutionContext context;
    private int showLimit;
    private int dumpLimit;

    public DumpStatementPostProcessor(ExecutionContext context, JdbcCompleter completer) {
        this.completer = completer;
        this.context = context;
        this.showLimit = completer.getConf().getRowMax();
        this.dumpLimit = Integer.parseInt(context.getOrElse(Constant.DUMP_LIMIT, Constant.DUMP_LIMIT_DEFAULT));
    }

    @Override
    public InterpreterResult execute(Statement statement) throws Exception {
        return someRowsAndWrite2TempFile(statement);
    }

    private InterpreterResult someRowsAndWrite2TempFile(Statement statement) throws IOException {
        try (ResultSet resultSet = statement.getResultSet()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<String> labels = SimpleQueryStatementPostProcessor.prepareColumnLabels(metaData);
            List<List<Object>> rows = getFirstNRowsAndDumpFile(resultSet, labels);
            return new InterpreterResultBuilder().success().table(labels, rows, rows.size()).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return new InterpreterResultBuilder().error().because(e.getMessage(), e);
        }
    }

    private List<List<Object>> getFirstNRowsAndDumpFile(ResultSet resultSet, List<String> labels) throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        TempFile dumpTarget = context.getDumpFileService()
                .newFile(context.getInterpreter().getUserName(), context.getJobId());
        dumpTarget.create();
        dumpTarget.write(new WriteResultWhileDump(labels, resultSet, rows, showLimit, dumpLimit));
        return rows;
    }

    public static class WriteResultWhileDump implements Consumer<Writer> {

        private final List<String> labels;
        private final ResultSet resultSet;
        private final List<List<Object>> rows;
        private final int showLimit;
        private final int dumpLimit;

        public WriteResultWhileDump(List<String> labels, ResultSet resultSet, List<List<Object>> rows,
                                    int showLimit, int dumpLimit) {
            this.labels = labels;
            this.resultSet = resultSet;
            this.rows = rows;
            this.showLimit = showLimit;
            this.dumpLimit = dumpLimit;
        }

        @Override
        public void accept(Writer writer) {

            CSVWriter csvWriter = new CSVWriter(writer,
                    DUMP_CSV_SEPARATOR, DUMP_CSV_QUOTE, DUMP_CSV_ESCAPE, DUMP_CSV_LINE_END);
            csvWriter.writeNext(labels.stream().map(String::toString).toArray(String[]::new), false);
            int dumpCount = 0;
            while (true) {
                try {
                    if (!resultSet.next()) break;
                    List<Object> row = new ArrayList<>();
                    for (int i = 0; i < labels.size(); i++) {
                        row.add(resultSet.getObject(i + 1));
                    }
                    if (row.size() == labels.size()) {
                        if (rows.size() < showLimit) {
                            rows.add(row);
                        }
                        if (dumpCount < dumpLimit) {
                            String[] csvRow = row.stream()
                                    .map(o -> Objects.isNull(o) ? "" : o.toString())
                                    .toArray(String[]::new);
                            csvWriter.writeNext(csvRow, false);
                            dumpCount++;
                        } else {
                            break;
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

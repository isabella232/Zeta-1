package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.RFC4180Parser;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.LivyResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.TimeoutResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.QueueAccessDeniedResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.SparkDriverErrorResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.UnknownQueueResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.SomeExceptionResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.ClusterAccessDeniedResponse;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.UnknownExceptionResponse;

import static com.ebay.dss.zds.interpreter.output.ResultEnum.ResultType.TABLE;

import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;

public class LivyResultParser {

    private static final JsonParser parser=new JsonParser();
    private static final List<LivyResponse> connectionResponse=Arrays.asList(
            // please make sure the rules are not collapsed and if so please keep this list in order
            new TimeoutResponse(),
            new QueueAccessDeniedResponse(),
            new UnknownQueueResponse(),
            new ClusterAccessDeniedResponse(),
            new SparkDriverErrorResponse(),
            new SomeExceptionResponse(),
            new UnknownExceptionResponse());

    public enum SqlType{
        SHOW_CREATE,
        EXPLAIN,
        TABLE,
        OTHERS
    }

    public static JsonObject parseCSVOutput(String output) {
        JsonObject result = new JsonObject();
        result.addProperty("type", TABLE.getName());
        JsonArray rows = new JsonArray();
        JsonArray columns=new JsonArray();
        result.add("schema",columns);
        result.add("rows", rows);

        if (StringUtils.isEmpty(output)) return result;
        RFC4180Parser csvParser = new RFC4180Parser();
        String[] csv = output.split("\n");

        if (csv.length >= 1) {
            try {
                String[] fields = csvParser.parseLine(csv[0]);

                /**add column names**/
                int cC = 0;
                String[] vColumns = new String[fields.length];
                for (String fieldName : fields) {
                    columns.add(fieldName);
                    vColumns[cC] = "c_" + cC++;
                }

                for (int i = 1; i < csv.length; i++) {
                    JsonObject jo = new JsonObject();
                    String[] row =  csvParser.parseLine(csv[i]);
                    int rowLength = row.length;

                    if (rowLength <= columns.size()) {
                        for (int j = 0; j < rowLength; j++) {
                            jo.addProperty(vColumns[j], row[j]);
                        }

                        // handle the empty string at the tail
                        for (int j = rowLength; j < columns.size(); j++) {
                            jo.addProperty(vColumns[j], "");
                        }
                    } else {
                        for (int j = 0; j < columns.size(); j++) {
                            jo.addProperty(vColumns[j], row[j]);
                        }
                    }
                    /**add one row**/
                    rows.add(jo);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return result;
        } else return result;
    }


    public static SqlType getSQLType(String originCode){
        String code=originCode.trim().toLowerCase().replaceAll("spark\\.sql\\(|sqlcontext\\.sql\\(|\"","");
        if(code.trim().startsWith("explain")){
            return SqlType.EXPLAIN;
        }else if(Pattern.compile("^show.*?create").matcher(code.trim()).find()){
            return SqlType.SHOW_CREATE;
        }else{
            return SqlType.TABLE;
        }
    }

    public static List<String> parseSQLOutput(SqlType type, String output) throws Exception {
        if (type.equals(SqlType.TABLE)) {
            return parseTable(output);
        } else if (type.equals(SqlType.SHOW_CREATE)) {
            return parseShowTable(output);
        } else if(type.equals(SqlType.EXPLAIN)){
            return parseExplain(output);
        }else{
            return parseTable(output);
        }
    }

    public static JsonObject parseJsonSQLOutput(String output) throws Exception {
        JsonObject result = new JsonObject();
        result.addProperty("type", TABLE.getName());
        JsonArray rows = new JsonArray();
        JsonArray columns=new JsonArray();

        JsonObject jOutput=parser.parse(output).getAsJsonObject();
        JsonArray jFields=jOutput.getAsJsonObject("schema").getAsJsonArray("fields");
        JsonArray jData=jOutput.getAsJsonArray("data");

        /**add column names**/
        int cC=0;
        String[] vColumns=new String[jFields.size()];
        for(JsonElement je:jFields){
            columns.add(je.getAsJsonObject().get("name").getAsString());
            vColumns[cC]="c_"+cC++;
        }
        for(JsonElement jRow: jData){
            JsonObject jo = new JsonObject();
            JsonArray ja=jRow.getAsJsonArray();
            for (int j = 0; j < columns.size(); j++) {
                jo.add(vColumns[j], ja.get(j));
            }
            /**add one row**/
            rows.add(jo);
        }
        result.add("schema",columns);
        result.add("rows", rows);
        return result;
    }

    public static JsonObject parseSQLOutput(String output) {
        JsonObject result = new JsonObject();
        result.addProperty("type", TABLE.getName());
        // Get first line by breaking on \n. We can guarantee
        // that \n marks the end of the first line, but not for
        // subsequent lines (as it could be in the cells)
        String firstLine = output.split("\n", 2)[0];
        // at least 4 lines, even for empty sql output
        //    +---+---+
        //    |  a|  b|
        //    +---+---+
        //    +---+---+

        // use the first line to determine the position of each cell
        String[] tokens = StringUtils.split(firstLine, "\\+");
        // pairs keeps the start/end position of each cell. We parse it from the first row
        // which use '+' as separator
        List<Pair> pairs = new ArrayList<>();
        int start = 0;
        int end = 0;
        for (String token : tokens) {
            start = end + 1;
            end = start + token.length();
            pairs.add(new Pair(start, end));
        }

        // Use the header line to determine the position
        // of subsequent lines
        int lineStart = 0;
        int lineEnd = firstLine.length();

        boolean isSchema=true;
        JsonArray rows = new JsonArray();
        JsonArray columns=new JsonArray();
        while (lineEnd < output.length()) {
            // Only match format "|....|"
            // skip line like "+---+---+" and "only showing top 1 row"
            String line = output.substring(lineStart, lineEnd);
            // Use the DOTALL regex mode to match newlines
            if (line.matches("(?s)^\\|.*\\|$")) {
                List<String> cells = new ArrayList<>();
                for (Pair pair : pairs) {
                    // strip the blank space around the cell and escape the string
                    cells.add(line.substring(pair.start, pair.end).trim());
                }
                if(isSchema) {
                    isSchema=false;
                    cells.forEach(name->columns.add(name));
                }else{
                    JsonObject jo = new JsonObject();
                    for (int j = 0; j < columns.size(); j++) {
                        String value=j>=cells.size()?"":cells.get(j);
                        jo.addProperty(columns.get(j).getAsString(),value);
                    }
                    rows.add(jo);
                }
            }
            // Determine position of next line skipping newline
            lineStart += firstLine.length() + 1;
            lineEnd = lineStart + firstLine.length();
        }
        result.add("schema",columns);
        result.add("rows", rows);
        return result;
    }

    public static List<String> parseTable(String output) throws Exception {
        List<String> rows = new ArrayList<>();
        // Get first line by breaking on \n. We can guarantee
        // that \n marks the end of the first line, but not for
        // subsequent lines (as it could be in the cells)
        String firstLine = output.split("\n", 2)[0];
        // at least 4 lines, even for empty sql output
        //    +---+---+
        //    |  a|  b|
        //    +---+---+
        //    +---+---+

        // use the first line to determine the position of each cell
        String[] tokens = StringUtils.split(firstLine, "\\+");
        // pairs keeps the start/end position of each cell. We parse it from the first row
        // which use '+' as separator
        List<Pair> pairs = new ArrayList<>();
        int start = 0;
        int end = 0;
        for (String token : tokens) {
            start = end + 1;
            end = start + token.length();
            pairs.add(new Pair(start, end));
        }

        // Use the header line to determine the position
        // of subsequent lines
        int lineStart = 0;
        int lineEnd = firstLine.length();
        while (lineEnd < output.length()) {
            // Only match format "|....|"
            // skip line like "+---+---+" and "only showing top 1 row"
            String line = output.substring(lineStart, lineEnd);
            // Use the DOTALL regex mode to match newlines
            if (line.matches("(?s)^\\|.*\\|$")) {
                List<String> cells = new ArrayList<>();
                for (Pair pair : pairs) {
                    // strip the blank space around the cell and escape the string
                    cells.add(escapeJavaScript(line.substring(pair.start, pair.end)).trim());
                }
                rows.add(StringUtils.join(cells, "\t"));
            }
            // Determine position of next line skipping newline
            lineStart += firstLine.length() + 1;
            lineEnd = lineStart + firstLine.length();
        }
        return rows;
    }

    public static List<String> parseExplain(String output) throws Exception {
        List<String> rows = new ArrayList<>();
        String[] lines = output.split("\n");
        StringBuilder sb = new StringBuilder();
        rows.add(lines[1].replaceAll("\\|","").trim());
        for (int i = 3; i < lines.length - 1; i++) {
            if (i == 3) {
                sb.append(lines[i].substring(1)).append("\n\r");
            } else if(i==lines.length - 2){
                sb.append(lines[i].substring(0,lines[i].length()-1));
            } else{
                sb.append(lines[i]).append("\n\r");
            }
        }
        rows.add(sb.toString());
        return rows;
    }

    public static List<String> parseShowTable(String output) throws Exception {
        List<String> rows = new ArrayList<>();
        String[] lines = output.split("\n");
        StringBuilder sb = new StringBuilder();
        rows.add("Schema");
        for (int i = 3; i < lines.length - 2; i++) {
            if (i == 3) {
                sb.append(lines[i].substring(1)).append("\n\r");
            } else {
                sb.append(lines[i]).append("\n\r");
            }
        }
        rows.add(sb.toString());
        return rows;
    }

    public static String extractErrorCause(String log){
        for(LivyResponse res: connectionResponse){
            String response = res.extractKeyWords(log);
            if(StringUtils.isNotEmpty(response)) return response;
        }
        return log;
    }

    private static class Pair {
        private int start;
        private int end;

        public Pair(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}

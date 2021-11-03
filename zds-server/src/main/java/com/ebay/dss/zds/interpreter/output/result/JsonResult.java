package com.ebay.dss.zds.interpreter.output.result;

import com.ebay.dss.zds.interpreter.interpreters.livy.LivyResultParser;
import com.ebay.dss.zds.interpreter.output.ResultEnum.ResultType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by tatian on 2018/4/18.
 */
public class JsonResult extends CommonResult {

    private JsonArray resultList;

    private static transient JsonParser jsonParser=new JsonParser();

    private JsonObject jo = new JsonObject();

    public JsonResult() {
    }

    public JsonResult(InterpreterResult itRet) {
        super(itRet);
        this.resultList = generateResultList(itRet);
    }

    public JsonObject parse(InterpreterResultMessage message) {
        JsonObject result;
        ResultType type = getResultType(message.getType());

        if (type.equals(ResultType.TABLE)) {
            result=jsonParser.parse(message.getData()).getAsJsonObject();
        } else if (type.equals(ResultType.CSV)) {
            result= LivyResultParser.parseCSVOutput(message.getData());
        }else {
            result = new JsonObject();
            result.addProperty("type", type.getName());
            if(!type.equals(ResultType.IGNORED)) {
                result.addProperty("content", message.getData());
            }
        }
        return result;
    }

    /*
    Parse zeppelin interpreter result
    public JsonObject parse(InterpreterResultMessage message) {
        JsonObject result = new JsonObject();
        ResultType type = getResultType(message.getType());
        result.addProperty("type", type.getName());

        if (type.equals(ResultType.TABLE)) {
            String[] data = message.getData().split(ROW_SEP);
            String[] columnNames = data[0].split(COL_SEP);
            JsonArray rows = new JsonArray();
            JsonArray columns=new JsonArray();

            Arrays.stream(columnNames).forEach(name->columns.add(name));

            for (int i = 1; i < data.length; i++) {
                JsonObject jo = new JsonObject();
                String[] row = data[i].split(COL_SEP);
                for (int j = 0; j < columnNames.length; j++) {
                    jo.addProperty(columnNames[j], j>=row.length?"":row[j]);
                }
                rows.add(jo);
            }
            result.add("schema",columns);
            result.add("rows", rows);
        } else {
            if(!type.equals(ResultType.IGNORED)) {
                result.addProperty("content", message.getData());
            }
        }
        return result;
    }*/

    public String get() {
        jo.add("header", getHeader());
        jo.add("result", resultList);
        return jo.toString();
    }

    public static String toJson(InterpreterResult itRet){
        return new JsonResult(itRet).get();
    }

    public void set(InterpreterResult itRet) {
        setHeader(generateHeader(itRet));
        this.resultList = generateResultList(itRet);
    }

    public JsonArray generateResultList(InterpreterResult itRet) {
        JsonArray ja = new JsonArray();
        for (InterpreterResultMessage message : itRet.message()) {
            ja.add(parse(message));
        }
        return ja;
    }


    public JsonArray getResultList() {
        return resultList;
    }

    public JsonResult setProperty(String key,String value){
        jo.addProperty(key, value);
        return this;
    }

    public static int getFactor(JsonArray origin) {
        return origin.size() / 2;
    }

    public JsonResult limit(int limit) {
        limitResult(resultList, limit);
        return this;
    }

    public static JsonArray limitArray(JsonArray origin, int limit) {
        if (origin == null || limit < 0 || limit >= origin.size()) return origin;
        if (limit <= getFactor(origin)) {
            JsonArray ja = new JsonArray();
            for (int i = 0; i < limit; i++) {
                ja.add(origin.get(i));
            }
            return ja;
        } else {
            for (int i = origin.size() - 1; i >= limit; i--) {
                origin.remove(i);
            }
            return origin;
        }
    }

    public static String getLimited(String json, int limit) {
        JsonObject jo = jsonParser.parse(json).getAsJsonObject();
        if (!jo.has("result")) return json;
        try {
            JsonArray result = jo.getAsJsonArray("result");
            boolean hasTable = false;
            int index = 0;
            for (; index < result.size() ; index++) {
                hasTable = "TABLE".equals(result
                        .get(index)
                        .getAsJsonObject()
                        .get("type")
                        .getAsString());
                if(hasTable) break;
            }
            if (hasTable) {
                JsonObject table = result.get(index).getAsJsonObject();
                JsonArray rows = table.getAsJsonArray("rows");
                rows = limitArray(rows, limit);
                table.add("rows", rows);
            } else return json;
        } catch (Exception ex) {
            ex.printStackTrace();
            return json;
        }
        return jo.toString();
    }

    public static JsonArray limitResult(JsonArray ja, int limit) {
        boolean hasTable = false;
        int index = 0;
        for (; index < ja.size() ; index++) {
            hasTable = "TABLE".equals(ja
                    .get(index)
                    .getAsJsonObject()
                    .get("type")
                    .getAsString());
            if(hasTable) break;
        }
        if (hasTable) {
            JsonObject table = ja.get(index).getAsJsonObject();
            JsonArray rows = table.getAsJsonArray("rows");
            rows = limitArray(rows, limit);
            table.add("rows", rows);
        }
        return ja;
    }

    public static JsonObject getLimited(JsonObject jo, int limit) {
        if (!jo.has("result")) return jo;
        try {
            JsonArray result = jo.getAsJsonArray("result");
            limitResult(result, limit);
        } catch (Exception ex) {
            ex.printStackTrace();
            return jo;
        }
        return jo;
    }

    public static String toCsv(String json, boolean withHeader) {
        if (StringUtils.isEmpty(json)) return "The result is empty";
        JsonObject jo = jsonParser.parse(json).getAsJsonObject();
        if (!jo.has("result")) return json;
        StringBuilder sb = new StringBuilder();
        try {
            JsonArray result = jo.getAsJsonArray("result");
            boolean hasTable = false;
            int index = 0;
            for (; index < result.size() ; index++) {
                hasTable = "TABLE".equals(result
                        .get(index)
                        .getAsJsonObject()
                        .get("type")
                        .getAsString());
                if(hasTable) break;
            }
            if (hasTable) {
                JsonObject table = result.get(index).getAsJsonObject();
                JsonArray rows = table.getAsJsonArray("rows");

                JsonArray schema = table.getAsJsonArray("schema");
                int fieldSize = schema.size();
                // append schema first
                String[] fieldList = new String[fieldSize];
                if (withHeader) {
                    for (int i = 0; i < fieldSize; i++) {
                        String fieldName = schema.get(i).getAsString();
                        sb.append(fieldName);
                        if (i < fieldSize - 1) {
                            sb.append(",");
                        } else sb.append("\n");
                        fieldList[i] = fieldName;
                    }
                }

                rows.forEach(je -> {
                    JsonObject row = je.getAsJsonObject();
                    Iterator<Map.Entry<String, JsonElement>> it = row.entrySet().iterator();
                    int idx = 0;
                    while (it.hasNext()) {
                        sb.append(it.next().getValue());
                        if (idx < fieldSize - 1) {
                            sb.append(",");
                        } else sb.append("\n");
                        idx++;
                    }
                });

            } else return json;
        } catch (Exception ex) {
            ex.printStackTrace();
            return json;
        }
        return sb.toString();
    }

    public static String getResultOfXY(JsonArray ja, int x, int y) {
        boolean hasTable = false;
        int index = 0;
        for (; index < ja.size() ; index++) {
            hasTable = "TABLE".equals(ja
                    .get(index)
                    .getAsJsonObject()
                    .get("type")
                    .getAsString());
            if(hasTable) break;
        }
        if (hasTable) {
            JsonObject table = ja.get(index).getAsJsonObject();
            JsonArray rows = table.getAsJsonArray("rows");
            if (x < rows.size()) {
                JsonObject je = rows.get(x).getAsJsonObject();
                String y_index = "c_" + y;
                JsonElement y_value = je.get(y_index);
                if (y_value != null && !y_value.isJsonNull()) {
                    return je.get(y_index).getAsString();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getResultOfXY(int x, int y) {
       return getResultOfXY(resultList, x, y);
    }

    public static JsonParser getParser() {
        return jsonParser;
    }

}

package com.ebay.dss.zds.interpreter.output;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InterpreterResultBuilder {

    private InterpreterResult.Code code;
    private List<String> contents = new LinkedList<>();
    private List<InterpreterResultMessage> messages = new LinkedList<>();
    private List<InterpreterResultMessageBuilderChain> messagesChain = new LinkedList<>();

    public TableMessageBuilder table() {
        return new TableMessageBuilder(this);
    }

    public InterpreterResultBuilder table(List<String> header, List<List<Object>> records) {
        return new TableMessageBuilder(this)
                .setHeader(header)
                .setRecords(records)
                .next();
    }

    public InterpreterResultBuilder table(List<String> header, List<List<Object>> records, int count) {
        return new TableMessageBuilder(this)
                .setHeader(header)
                .setRecords(records)
                .setCount(count)
                .next();
    }

    public InterpreterResultBuilder table(List<String> header, List<List<Object>> records, int count, long updatedCount) {
        return new TableMessageBuilder(this)
                .setHeader(header)
                .setRecords(records)
                .setCount(count)
                .setUpdatedCount(updatedCount)
                .next();
    }

    public InterpreterResultBuilder simple(InterpreterResult.Type type, String data) {
        return new SimpleMessageBuilder(this)
                .setType(type)
                .setData(data)
                .next();
    }

    public SimpleMessageBuilder simple() {
        return new SimpleMessageBuilder(this);
    }

    public InterpreterResultBuilder text(String data) {
        return simple(InterpreterResult.Type.TEXT, data);
    }

    public InterpreterResultBuilder image(String data) {
        return simple(InterpreterResult.Type.IMG, data);
    }

    public InterpreterResultBuilder html(String data) {
        return simple(InterpreterResult.Type.HTML, data);
    }

    public InterpreterResultBuilder messages(List<InterpreterResultMessage> messages) {
        this.messages.addAll(messages);
        return this;
    }

    public InterpreterResultBuilder success() {
        this.code = InterpreterResult.Code.SUCCESS;
        return this;
    }

    public InterpreterErrorResultBuilder error() {
        return new InterpreterErrorResultBuilder();
    }

    public InterpreterResultBuilder content(String content) {
        this.contents.add(content);
        return this;
    }

    public InterpreterResultBuilder code(InterpreterResult.Code code) {
        this.code = code;
        return this;
    }

    public InterpreterResult emptySuccess() {
        return this.success().content("").build();
    }

    private boolean nonNullAndNonLengthZero(List list) {
        return Objects.nonNull(list) && list.size() > 0;
    }

    public InterpreterResult build() {
        InterpreterResult result = new InterpreterResult(code);
        if (nonNullAndNonLengthZero(contents)) {
            contents.stream().filter(StringUtils::isNotBlank).forEach(result::add);
        }
        List<InterpreterResultMessage> finalMessages = new LinkedList<>();
        List<InterpreterResultMessage> chainResult = messagesChain.stream()
                .map(InterpreterResultMessageBuilderChain::build)
                .collect(Collectors.toList());
        finalMessages.addAll(messages);
        finalMessages.addAll(chainResult);
        if (finalMessages.size() > 0) {
            finalMessages.stream().filter(Objects::nonNull).forEach(result::add);
        }
        return result;
    }

    private InterpreterResultBuilder next(InterpreterResultMessageBuilderChain builderChain) {
        messagesChain.add(builderChain);
        return this;
    }

    public interface InterpreterResultMessageBuilderChain {
        InterpreterResultMessage build();

        InterpreterResultBuilder next();
    }

    public static class TableMessageBuilder implements InterpreterResultMessageBuilderChain {
        private InterpreterResultBuilder resultBuilder;
        private List<String> header;
        private List<List<Object>> records;
        private int count = 0;
        private long updatedCount = 0;
        private

        TableMessageBuilder(InterpreterResultBuilder resultBuilder) {
            this.resultBuilder = resultBuilder;
        }

        public TableMessageBuilder setCount(int count) {
            this.count = count;
            return this;
        }

        public TableMessageBuilder setUpdatedCount(long updatedCount) {
            this.updatedCount = updatedCount;
            return this;
        }

        public TableMessageBuilder setHeader(List<String> header) {
            this.header = header;
            return this;
        }

        public TableMessageBuilder setRecords(List<List<Object>> records) {
            this.records = records;
            return this;
        }

        @Override
        public InterpreterResultMessage build() {
            JsonObject jsonTable = new JsonObject();
            jsonTable.addProperty("type", "TABLE");

            JsonArray jsonHeader = new JsonArray();
            if (Objects.nonNull(header)) {
                header.forEach(jsonHeader::add);
            }
            jsonTable.add("schema", jsonHeader);

            JsonArray valuesArray = new JsonArray();
            if (Objects.nonNull(records)) {
                records.forEach(recordValues -> {
                    JsonObject oneValueObject = new JsonObject();
                    for (int i = 0; i < recordValues.size(); i++) {
                        Object value = recordValues.get(i);
                        oneValueObject.addProperty("c_" + i, value == null? null : String.valueOf(value));
                    }
                    valuesArray.add(oneValueObject);
                });
            }
            jsonTable.add("rows", valuesArray);

            jsonTable.addProperty("count", count);
            jsonTable.addProperty("updatedCount", updatedCount);

            return new InterpreterResultMessage(InterpreterResult.Type.TABLE, jsonTable.toString());
        }

        public InterpreterResultBuilder next() {
            return resultBuilder.next(this);
        }
    }

    public static class SimpleMessageBuilder implements InterpreterResultMessageBuilderChain {
        private InterpreterResultBuilder resultBuilder;
        private InterpreterResult.Type type = InterpreterResult.Type.TEXT;
        private String data;

        private SimpleMessageBuilder(InterpreterResultBuilder resultBuilder) {
            this.resultBuilder = resultBuilder;
        }

        public SimpleMessageBuilder setType(InterpreterResult.Type type) {
            this.type = type;
            return this;
        }

        public SimpleMessageBuilder setData(String data) {
            this.data = data;
            return this;
        }

        @Override
        public InterpreterResultMessage build() {
            return new InterpreterResultMessage(type, data);
        }

        @Override
        public InterpreterResultBuilder next() {
            return resultBuilder.next(this);
        }
    }

    public static class InterpreterErrorResultBuilder {

        public InterpreterResult closed() {
            return new InterpreterResult(InterpreterResult.Code.CLOSED, "Interpreter is closed");
        }

        public InterpreterResult cancelled() {
            return because("Job is cancelled");
        }

        public InterpreterResult emptyResult() {
            return because("Empty output");
        }

        public InterpreterResult because(String reason) {
            return new InterpreterResult(InterpreterResult.Code.ERROR, StringUtils.isEmpty(reason)?
                    "Can't get specific error messages, please check server logs for details" : reason);
        }

        public InterpreterResult because(String message, Throwable e) {
            StringBuilder reasonBuilder = new StringBuilder();
            reasonBuilder.append(message);
            Throwable current = e;
            Throwable cause = e.getCause();
            while (cause != null) {
                reasonBuilder.append(current.getMessage()).append(" <- ");
                current = cause;
                cause = current.getCause();
            }
            reasonBuilder.append(current.getMessage());
            return because(reasonBuilder.toString());
        }

        public InterpreterResult rootCause(Throwable e) {
            Throwable current = e;
            Throwable cause = e.getCause();
            while (cause != null) {
                current = cause;
                cause = current.getCause();
            }
            return because(current.getMessage());
        }
    }


}

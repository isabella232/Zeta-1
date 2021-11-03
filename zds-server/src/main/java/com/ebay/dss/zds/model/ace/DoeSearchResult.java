package com.ebay.dss.zds.model.ace;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class DoeSearchResult extends DoeSearchObjectBase {

    private Value value;

    public Value getValue() {
        return value;
    }

    public DoeSearchResult setValue(Value value) {
        this.value = value;
        return this;
    }

    public static class Value  {
        @JsonProperty("total_size")
        private Integer totalSize;
        private Integer offset;
        private List<Result> result;

        public Integer getTotalSize() {
            return totalSize;
        }

        public Value setTotalSize(Integer totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public Integer getOffset() {
            return offset;
        }

        public Value setOffset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public List<Result> getResult() {
            return result;
        }

        public Value setResult(List<Result> result) {
            this.result = result;
            return this;
        }
    }

    public static class Result {
        private String type;
        private String name;
        private float score;
        private String content;

        public String getType() {
            return type;
        }

        public Result setType(String type) {
            this.type = type;
            return this;
        }

        public String getName() {
            return name;
        }

        public Result setName(String name) {
            this.name = name;
            return this;
        }

        public float getScore() {
            return score;
        }

        public Result setScore(float score) {
            this.score = score;
            return this;
        }

        public String getContent() {
            return content;
        }

        public Result setContent(String content) {
            this.content = content;
            return this;
        }
    }
}

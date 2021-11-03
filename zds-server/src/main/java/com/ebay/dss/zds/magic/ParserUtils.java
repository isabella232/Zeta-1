package com.ebay.dss.zds.magic;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2019-10-23.
 */
public class ParserUtils {

  public static final class SplitSQL {
    public final String sql;

    private List<SQLSegment> splitedSql = new ArrayList<>();
    private List<Partition> commentPos = new ArrayList<>();
    private List<Integer> revertSlashes = new ArrayList<>();
    private ParseListener listener;
    private boolean split = false;
    private boolean trimResult = false;
    private boolean omitEmptyStrings = false;
    private boolean removeComment = false;
    private boolean inSingleQuote = false;
    private boolean inComment = false;
    private String commentType = "";
    private static final String DOUBLE_BAR = "--";
    private static final String SLASH = "/*";
    private static final String NO_COMMENT = "no_comment";

    private int lastRevertSlash = -1;

    public SplitSQL(String fullSQL) {
      this.sql = fullSQL;
    }

    public SplitSQL trimResult() {
      this.trimResult = true;
      return this;
    }

    public SplitSQL omitEmptyStrings() {
      this.omitEmptyStrings = true;
      return this;
    }

    public SplitSQL removeComment() {
      this.removeComment = true;
      return this;
    }

    private SQLSegment cleanComment(String line, int lineStart, int lineEnd) {
      SQLSegment segment = new SQLSegment(new SplitContext(removeComment, trimResult));
      List<Partition> relativeCommentsPos = new ArrayList<>();
      segment.originalSQL = line;
      segment.commentPos = relativeCommentsPos;
      StringBuilder newLine = new StringBuilder();
      int lastEndBorder = -1;
      // this commentPos is already ordered
      for (Partition partition : commentPos) {
        if (partition.in(lineStart, lineEnd)) {
          Partition relativePartition = partition.relative(lineStart, lineEnd);
          relativeCommentsPos.add(relativePartition);
          newLine.append(line.substring(lastEndBorder + 1, relativePartition.begin));
          lastEndBorder = relativePartition.end;
        }
      }

      if (lastEndBorder < line.length() - 1) {
        newLine.append(line.substring(lastEndBorder + 1));
      }
      segment.handledSQL = newLine.toString();
      return segment;
    }

    public boolean isCommentScope(int start, int end) {
      for (Partition partition : commentPos) {
        if (partition.contains(start, end)) {
          return true;
        }
      }
      return false;
    }

    private boolean validRevertSlash(int index) {
      for (int i = revertSlashes.size() - 1; i >= 0; i--) {
        if (revertSlashes.get(i).equals(index)) return true;
      }
      return false;
    }

    public void setParseListener(ParseListener listener) {
      this.listener = listener;
    }

    private void onNewSql(String sql, int index, List<SQLSegment> currentSQLs) {
      if (this.listener != null) {
        try {
          this.listener.onNewSql(sql, index, currentSQLs);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }

    private void addSql(SQLSegment segment) {
      this.splitedSql.add(segment);
      onNewSql(segment.handledSQL, splitedSql.size() - 1, splitedSql);
    }

    public void parse() {
      if (!split) {
        int totalLength = sql.length();
        int lastSepIndex = 0;
        for (int i = 0; i < totalLength; i++) {
          char currentChar = sql.charAt(i);
          switch (currentChar) {
            case '\'':
              if (i == 0) {
                inSingleQuote = true;
              } else {
                if (!inComment) {
                  if (!validRevertSlash(i - 1)) { // sql.charAt(i - 1) != '\\'
                    if (inSingleQuote) {
                      inSingleQuote = false;
                    } else {
                      inSingleQuote = true;
                    }
                  }
                }
              }
              break;
            case '\\':
              if (!inComment && !validRevertSlash(i - 1)) {
                lastRevertSlash = i;
                revertSlashes.add(i);
              }
              break;
            case '/':
              if (i == 0) {
                if (sql.length() > 1 && sql.charAt(i + 1) == '*') {
                  inComment = true;
                  commentType = SLASH;
                  commentPos.add(new Partition(i, totalLength - 1));
                }
              } else {
                if (!inSingleQuote && inComment && commentType.equals(SLASH)) {
                  if (sql.charAt(i - 1) == '*') {
                    if (i == 1) {
                      inComment = false;
                      commentType = NO_COMMENT;
                      Partition comment = commentPos.get(commentPos.size() - 1);
                      comment.end = i;
                    } else {
                      if (!validRevertSlash(i - 2)) {
                        inComment = false;
                        commentType = NO_COMMENT;
                        Partition comment = commentPos.get(commentPos.size() - 1);
                        comment.end = i;
                      }
                    }
                  }
                } else if (!inSingleQuote && !inComment) {
                  if (!validRevertSlash(i - 1)) {
                    if (i < sql.length() - 1 && sql.charAt(i + 1) == '*') {
                      inComment = true;
                      commentType = SLASH;
                      commentPos.add(new Partition(i, totalLength - 1));
                    }
                  }
                }
              }
              break;
            case '-':
              if (i == 0) {
                if (sql.length() > 1 && sql.charAt(i + 1) == '-') {
                  inComment = true;
                  commentType = DOUBLE_BAR;
                  commentPos.add(new Partition(i, totalLength - 1));
                }
              } else {
                if (!inSingleQuote && !inComment) {
                  if (!validRevertSlash(i - 1)) {
                    if (i < sql.length() - 1 && sql.charAt(i + 1) == '-') {
                      inComment = true;
                      commentType = DOUBLE_BAR;
                      commentPos.add(new Partition(i, totalLength - 1));
                    }
                  }
                }
              }
              break;
            case '\n':
              if (i > 0 && inComment && commentType.equals(DOUBLE_BAR) && !validRevertSlash(i - 1)) {
                inComment = false;
                commentType = NO_COMMENT;
                Partition comment = commentPos.get(commentPos.size() - 1);
                comment.end = i - 1; // not include \n
              }
              break;
            case ';':
              if (!inSingleQuote && !inComment) {
                String line = sql.substring(lastSepIndex, i);
                SQLSegment segment = cleanComment(line, lastSepIndex, i - 1);
                if (removeComment) {
                  line = segment.handledSQL;
                }
                if (trimResult || omitEmptyStrings) {
                  line = StringUtils.trim(line);
                }
                segment.handledSQL = line;
                if (omitEmptyStrings && StringUtils.isNotEmpty(line)) {
                  addSql(segment);
                } else if (!omitEmptyStrings) {
                  addSql(segment);
                }
                lastSepIndex = i + 1;
              }
              break;
            default:
              break;
          }
        }

        if (lastSepIndex < sql.length()) {
          String line = sql.substring(lastSepIndex);
          SQLSegment segment = cleanComment(line, lastSepIndex, sql.length() - 1);
          if (removeComment) {
            line = segment.handledSQL;
          }
          if (trimResult || omitEmptyStrings) {
            line = StringUtils.trim(line);
          }
          segment.handledSQL = line;
          if (omitEmptyStrings && StringUtils.isNotEmpty(line)) {
            addSql(segment);
          } else if (!omitEmptyStrings) {
            addSql(segment);
          }
        }
        split = true;
      }
    }

    public List<String> get() {
      return getSegments()
              .stream()
              .map((SQLSegment segment) -> segment.handledSQL)
              .collect(Collectors.toList());
    }

    public List<SQLSegment> getSegments() {
      parse();
      return splitedSql;
    }
  }

  public static class SplitContext {
    public final boolean removeComment;
    public final boolean trimResult;

    public SplitContext(boolean removeComment, boolean trimResult) {
      this.removeComment = removeComment;
      this.trimResult = trimResult;
    }
  }

  public static class SQLSegment {
    public String originalSQL;
    public String handledSQL;
    // this should be ordered
    public final SplitContext splitContext;
    public List<Partition> commentPos;

    public SQLSegment(SplitContext splitContext) {
        this.splitContext = splitContext;
    }

    public SQLSegment(String originalSQL, String handledSQL, List<Partition> commentPos, SplitContext splitContext) {
      this.originalSQL = originalSQL;
      this.handledSQL = handledSQL;
      this.commentPos = commentPos;
      this.splitContext = splitContext;
    }
  }

  public static class Partition {
    public int begin;
    public int end;

    public Partition(int begin, int end) {
      this.begin = begin;
      this.end = end;
      assert end >= begin;
    }

    public boolean contains(int index) {
      return index >= begin && index <= end;
    }

    public boolean contains(int start, int end) {
      assert start <= end;
      return this.begin <= start && end <= this.end;
    }

    public boolean hasOverlap(int start, int end) {
      assert start <= end;
      return this.contains(start, end)
              || this.in(start, end)
              || (end >= this.begin && end <= this.end)
              || (start >= this.begin && start <= this.end);
    }

    public boolean in(int from, int to) {
      assert to >= from;
      return begin >= from && end <= to;
    }

    public Partition relative(int from, int to) {
      assert in(from, to);
      return new Partition(begin - from, end - from);
    }
  }

  public interface ParseListener {

    void onNewSql(String sql, int index, List<SQLSegment> currentSQLs);
  }
}

package com.ebay.dss.zds.common;

/**
 * Created by wenliu2 on 4/10/18.
 */
public class Constant {
    public static final String ZDS_SERVER_REQUEST_ID_ATTR = "zds-server-req-id";

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String DUMP_LIMIT = "dump_limit";
    public static final String VIEW_LIMIT = "view_limit";
    public static final String DEFAULT_LIMIT_KEY = "zds.view.limit.default";
    public static final String SHARED_LIVY_CODE_TYPE = "zds.livy.code.type";
    public static final String SHARED_SESSION_KIND = "shared";
    public static final String DUMP_LIMIT_DEFAULT = "1000000";
    public static final char DUMP_CSV_SEPARATOR = ',';
    public static final char DUMP_CSV_QUOTE = '"';
    public static final char DUMP_CSV_ESCAPE = '"';
    public static final String DUMP_CSV_LINE_END = "\n";
    public static final String ZDS_OPERATION_TRIGGER = "zds.operation.trigger";

    public static final Global GLOBAL = new Global();

    public static final class Global {
        public final String KEY_SEP = "_";

        private Global() {
        }

        public String workingUnitKey(String userId, String notebookId) {
            return userId + KEY_SEP + notebookId;
        }
    }
};

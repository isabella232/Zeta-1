package com.ebay.dss.zds.common;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.UnknownClusterException;

public class ClusterUtil {

    public static final int hercules = 10;
    public static final int hercules_sub = 11;
    public static final int apollophx = 3;
    public static final int areslvs = 2;

    public static String getHadoopConfDir(int clusterId) {

        // todo fulfill switch
        String dir;
        switch (clusterId) {
            case ClusterUtil.hercules:
                dir = "";
                break;
            case ClusterUtil.hercules_sub:
                dir = "";
                break;
            case ClusterUtil.apollophx:
                dir = "";
                break;
            case ClusterUtil.areslvs:
                dir = "";
                break;
            default:
                throw new UnknownClusterException(ErrorCode.INVALID_INPUT, "Cluster Id is not recognized");
        }
        return dir;
    }
}

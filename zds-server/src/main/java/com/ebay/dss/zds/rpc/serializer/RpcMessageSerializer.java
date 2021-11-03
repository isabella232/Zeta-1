package com.ebay.dss.zds.rpc.serializer;

import java.nio.ByteBuffer;

/**
 * Created by tatian on 2020-10-10.
 */
public interface RpcMessageSerializer {

    Object deserialize(ByteBuffer data);
    ByteBuffer serialize(Object data);
}

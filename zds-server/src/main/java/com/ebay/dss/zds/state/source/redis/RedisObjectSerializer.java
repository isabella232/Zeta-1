package com.ebay.dss.zds.state.source.redis;

import com.ebay.dss.zds.state.StateSnapshot;

import java.io.*;

/**
 * Created by tatian on 2020-09-17.
 */
public class RedisObjectSerializer {

  public static byte[] serialize(StateSnapshot object) {
    ObjectOutputStream oos = null;
    ByteArrayOutputStream baos = null;
    try {
      baos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
      return baos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (baos != null) {
          baos.close();
        }
        if (oos != null) {
          oos.close();
        }
      } catch (Exception e2) {
        e2.printStackTrace();
      }
    }
    return null;
  }


  public static <T extends Serializable> StateSnapshot<T> deserialize(byte[] bytes) {
    ByteArrayInputStream bais = null;
    ObjectInputStream ois = null;
    try {
      bais = new ByteArrayInputStream(bytes);
      ois = new ObjectInputStream(bais);
      return (StateSnapshot<T>) ois.readObject();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (ois != null) {
          ois.close();
        }
        if (bais != null) {
          bais.close();
        }
      } catch (Exception e2) {
        e2.printStackTrace();
      }
    }
    return null;
  }
}

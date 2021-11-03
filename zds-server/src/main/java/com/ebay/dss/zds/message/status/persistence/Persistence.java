package com.ebay.dss.zds.message.status.persistence;

/**
 * Created by tatian on 2019-07-24.
 */
public abstract class Persistence<T> {

    public abstract void persist(T element);
}

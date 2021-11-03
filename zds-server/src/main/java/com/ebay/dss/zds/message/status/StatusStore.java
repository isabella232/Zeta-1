package com.ebay.dss.zds.message.status;

import com.ebay.dss.zds.message.status.persistence.Persistence;

import java.util.Optional;

/**
 * Created by tatian on 2019-07-20.
 */
public abstract class StatusStore<T, V> {

    protected abstract Optional<V> flushBy(T event);

    protected abstract Persistence<V> getPersistence();

    public void flushAndPersist(T event) {
        Optional<V> entry = flushBy(event);
        Persistence<V> factory = getPersistence();
        if (factory != null && entry.isPresent()) factory.persist(entry.get());
    }
}

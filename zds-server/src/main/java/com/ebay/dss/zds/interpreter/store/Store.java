package com.ebay.dss.zds.interpreter.store;

import java.util.Collection;
import java.util.Set;

/**
 * Created by tatian on 2019-02-21.
 */
public interface Store<K, V> {

    V get(K key);
    void store(K key, V value);
    V remove(K key);
    void clear();
    int size();
    Set<K> keySet();
    Collection<V> values();

}

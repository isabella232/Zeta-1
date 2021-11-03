package com.ebay.dss.zds.interpreter.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by tatian on 2019-02-21.
 */
public class MemoryStore<K, V> implements Store<K, V> {

    private Map<K, V> store;

    public MemoryStore() {
        this.store = new HashMap<>();
    }

    public MemoryStore(Map<K, V> store) {
        this.store = store;
    }

    public V get(K key) {
        return store.get(key);
    }

    public void store(K key, V value) {
        store.put(key, value);
    }

    public V remove(K key) {
        return store.remove(key);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    public Set<K> keySet() {
        return store.keySet();
    }

    public Collection<V> values() {
        return store.values();
    }
}

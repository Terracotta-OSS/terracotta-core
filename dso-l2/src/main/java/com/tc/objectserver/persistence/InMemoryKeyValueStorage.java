package com.tc.objectserver.persistence;

import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.persistence.KeyValueStorage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author vmad
 */
public class InMemoryKeyValueStorage<K, V> implements KeyValueStorage<K, V>, StateDumpable {

    private final Map<K, V> delegate = new ConcurrentHashMap<>();

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public long size() {
        return delegate.size();
    }

    @Override
    public void put(K key, V value) {
        put(key, value, (byte)0);
    }

    @Override
    public void put(K key, V value, byte metadata) {
        delegate.put(key, value);
    }

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public boolean remove(K key) {
        return delegate.remove(key) != null;
    }

    @Override
    public void removeAll(Collection<K> keys) {
        for(K key : keys) {
            delegate.remove(key);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void dumpStateTo(StateDumper stateDumper) {
        stateDumper.dumpState("size", String.valueOf(delegate.size()));
    }
}

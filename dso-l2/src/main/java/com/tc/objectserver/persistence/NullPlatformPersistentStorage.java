package com.tc.objectserver.persistence;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author vmad
 */
public class NullPlatformPersistentStorage implements IPersistentStorage {

    final Map<String, String> properties = new ConcurrentHashMap<>();
    final Map<String, KeyValueStorage<?, ?>> maps = new ConcurrentHashMap<>();

    @Override
    public void open() throws IOException {
        //nothing to do
    }

    @Override
    public void create() throws IOException {
        //nothing to do
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public Transaction begin() {
        return new Transaction() {
            @Override
            public void commit() {
                //nothing to do
            }

            @Override
            public void abort() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public <K, V> KeyValueStorage<K, V> getKeyValueStorage(String alias, Class<K> keyClass, Class<V> valueClass) {
        maps.putIfAbsent(alias, new InMemoryKeyValueStorage<K, V>());
        return (KeyValueStorage<K, V>) maps.get(alias);
    }

    @Override
    public <K, V> KeyValueStorage<K, V> createKeyValueStorage(String alias, Class<K> keyClass, Class<V> valueClass) {
        maps.putIfAbsent(alias, new InMemoryKeyValueStorage<K, V>());
        return (KeyValueStorage<K, V>) maps.get(alias);
    }

    @Override
    public <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String alias) {
        return (KeyValueStorage<K, V>) maps.remove(alias);
    }
}

package org.terracotta.passthrough;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

/**
 * NOTE: This is loosely a clone of the NullPlatformPersistentStorage class in terracotta-core with some unused
 *  functionality stripped out.
 */
public class PassthroughNullPlatformPersistentStorage implements IPersistentStorage {
  final Map<String, InMemoryKeyValueStorage<?, ?>> maps = new HashMap<String, InMemoryKeyValueStorage<?, ?>>();

  @Override
  public void open() throws IOException {
    // nothing to do
  }

  @Override
  public void create() throws IOException {
    // nothing to do
  }

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public Transaction begin() {
    // We know that passthrough doesn't use this.
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> getProperties() {
    // We know that passthrough doesn't use this.
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <K, V> KeyValueStorage<K, V> getKeyValueStorage(String alias, Class<K> keyClass, Class<V> valueClass) {
    if (!this.maps.containsKey(alias)) {
      this.maps.put(alias, new InMemoryKeyValueStorage<K, V>());
    }
    return (KeyValueStorage<K, V>) this.maps.get(alias);
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <K, V> KeyValueStorage<K, V> createKeyValueStorage(String alias, Class<K> keyClass, Class<V> valueClass) {
    if (!this.maps.containsKey(alias)) {
      this.maps.put(alias, new InMemoryKeyValueStorage<K, V>());
    }
    return (KeyValueStorage<K, V>) this.maps.get(alias);
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String alias) {
    return (KeyValueStorage<K, V>) maps.remove(alias);
  }

  private static class InMemoryKeyValueStorage<K, V> implements KeyValueStorage<K, V>, StateDumpable {
    private final Map<K, V> delegate = new ConcurrentHashMap<K, V>();

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
      put(key, value, (byte) 0);
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
      for (K key : keys) {
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
}

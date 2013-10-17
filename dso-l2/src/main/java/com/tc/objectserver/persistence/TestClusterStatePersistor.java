package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author tim
 */
public class TestClusterStatePersistor extends ClusterStatePersistor {
  public TestClusterStatePersistor(final Map<String, String> map) {
    super(new StorageManager() {
      @Override
      public Map<String, String> getProperties() {
        return map;
      }

      @Override
      public <K, V> KeyValueStorage<K, V> getKeyValueStorage(final String alias, final Class<K> keyClass, final Class<V> valueClass) {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public void destroyKeyValueStorage(final String alias) {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public <K, V> KeyValueStorage<K, V> createKeyValueStorage(final String alias, final KeyValueStorageConfig<K, V> config) {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public void begin() {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public void commit() {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public Future<?> start() {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public void close() {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public Collection<MonitoredResource> getMonitoredResources() {
        throw new UnsupportedOperationException("Implement me!");
      }
    });
  }

  public TestClusterStatePersistor() {
    this(new HashMap<String, String>());
  }
}

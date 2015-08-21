package com.tc.objectserver.persistence;

import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;
/**
 * @author tim
 */
public class TestClusterStatePersistor extends ClusterStatePersistor {
  public TestClusterStatePersistor(Map<String, String> map) {
    super(new IPersistentStorage() {
      boolean isReady = false;
      
      @Override
      public void open() throws IOException {
        Assert.assertFalse(this.isReady);
        this.isReady = true;
      }

      @Override
      public void create() throws IOException {
        Assert.assertFalse(this.isReady);
        this.isReady = true;
      }

      @Override
      public Map<String, String> getProperties() {
        Assert.assertTrue(this.isReady);
        return map;
      }

      @Override
      public <K, V> KeyValueStorage<K, V> getKeyValueStorage(String alias, Class<K> keyClass, Class<V> valueClass) {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public void close() {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public IPersistentStorage.Transaction begin() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public <K, V> KeyValueStorage<K, V> createKeyValueStorage(String alias, Class<K> keyClass, Class<V> valueClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String alias) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
      
    });
  }

  public TestClusterStatePersistor() {
    this(new HashMap<String, String>());
  }
}

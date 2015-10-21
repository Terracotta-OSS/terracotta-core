/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

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

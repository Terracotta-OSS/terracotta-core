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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.IPlatformPersistence;
import org.terracotta.persistence.KeyValueStorage;

import com.tc.util.Assert;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implements an entire IPersistentStorage data store on top of an underlying IPlatformPersistence implementation.
 * NOTE:  This is just temporary until IPersistentStorage can be fully removed.
 */
public class EmulatedPersistentStorage implements IPersistentStorage, StateDumpable {
  private static final String FILE_NAME = "IPersistenceStorage.dat";
  private static final String KEY_PROPERTIES = "properties";
  private static final String KEY_MAPS = "maps";

  private final Object lock = new Object();
  private final IPlatformPersistence platformPersistence;
  private PersistentStorageProperties properties;
  private Map<String, SynchronizedKeyValueStorage<?, ?>> maps;
  
  private final FlushingWrite doFlush = new FlushingWrite() {
    @Override
    public <T> T run(Callable<T> r) {
      T result = null;
      try {
        synchronized (lock) {
            result = r.call();
            
            // Build the HashMap to write.
            HashMap<String, Serializable> map = new HashMap<>();
            map.put(KEY_PROPERTIES, properties);
            map.put(KEY_MAPS, (Serializable)maps);
            
            platformPersistence.storeDataElement(FILE_NAME, map);
        }
      } catch (Exception e) {
        // If something happened here, that is a serious bug so we need to assert.
        throw Assert.failure("Failure writing EmulatedPersistentStorage", e);
      }
      return result;
    }
  };
  
  public EmulatedPersistentStorage(IPlatformPersistence platformPersistence) {
    this.platformPersistence = platformPersistence;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void open() throws IOException {
    HashMap<String, Serializable> combinedMap = (HashMap<String, Serializable>) this.platformPersistence.loadDataElement(FILE_NAME);
    if (null == combinedMap) {
      // Treat this as a file not found - they should have used create.
      throw new IOException("not found");
    }
    
    this.properties = (PersistentStorageProperties) combinedMap.get(KEY_PROPERTIES);
    this.maps = (Map<String, SynchronizedKeyValueStorage<?, ?>>) combinedMap.get(KEY_MAPS);
    for (Map.Entry<String, SynchronizedKeyValueStorage<?, ?>> entry : maps.entrySet()) {
      entry.getValue().setFlushCallback(doFlush);
    }
    this.properties.setWriter(doFlush);
  }

  @Override
  public void create() throws IOException {
    this.properties = new PersistentStorageProperties(doFlush);
    this.maps = new ConcurrentHashMap<>();
    // Write the file, for the first time, so that we can attempt to open it later, even if we don't write anything.
    this.doFlush.run(()->null);
  }

  @Override
  public void close() {
    doFlush.run(()->null);
  }

  @Override
  public Map<String, String> getProperties() {
    return this.properties;
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized <K, V> KeyValueStorage<K, V> getKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    // It appears as though we often don't create these, ahead-of-time.
    synchronized (lock) {
    if (!maps.containsKey(name)) {
      SynchronizedKeyValueStorage<K, V> storage = new SynchronizedKeyValueStorage<>(doFlush);
      maps.put(name, storage);
    }
    return (KeyValueStorage<K, V>) maps.get(name);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <K, V> KeyValueStorage<K, V> createKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    synchronized (lock) {
    if (!maps.containsKey(name)) {
      SynchronizedKeyValueStorage<K, V> storage = new SynchronizedKeyValueStorage<>(doFlush);
      maps.put(name, storage);
      return storage;
    }
    return (KeyValueStorage<K, V>)maps.get(name);
    }
  }

  @Override
  public <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String name) {
    synchronized (lock) {
      @SuppressWarnings("unchecked")
      KeyValueStorage<K, V> storage =  (KeyValueStorage<K, V>)maps.get(name);
      maps.remove(name);
      return storage;
    }
  }  

  @Override
  public Transaction begin() {
    return new Transaction() {

      @Override
      public void commit() {
        doFlush.run(()->null);
      }

      @Override
      public void abort() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
  }


  @Override
  public void dumpStateTo(StateDumper stateDumper) {
    for (Map.Entry<String, SynchronizedKeyValueStorage<?, ?>> entry : maps.entrySet()) {
      entry.getValue().dumpStateTo(stateDumper.subStateDumper(entry.getKey()));
    }
  }
}

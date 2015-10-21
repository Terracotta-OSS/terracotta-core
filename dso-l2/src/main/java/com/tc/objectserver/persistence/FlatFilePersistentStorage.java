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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

import com.tc.util.Assert;


/**
 * Implements the simple key-value storage persistence system.  Note that any modifications made to the data stored within
 * this (via a returned key-value storage object) will invoke a flush of all data back to the disk.
 * NOTE:  the current implementation is NOT thread-safe so all consumers must be ensure serialized access to this object as
 * well as any key-value storage objects or properties maps it returns.
 */
public class FlatFilePersistentStorage implements IPersistentStorage {
  private final String path;
  private HashMap<String, String> properties;
  private HashMap<String, FlatFileKeyValueStorage<?, ?>> maps;
  
  private final Runnable doFlush = new Runnable() {
    @Override
    public synchronized void run() {
      try {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
        out.writeObject(properties);
        out.writeObject(maps);
        out.close();
      } catch (Exception e) {
        // If something happened here, that is a serious bug so we need to assert.
        Assert.failure("Failure flushing FlatFileKeyValueStorage", e);
      }
    }
  };
  
  public FlatFilePersistentStorage(String path) {
    this.path = path;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void open() throws IOException {
    // Note that we will fail out for FileNotFound and other IOExceptions since those are the checked kinds of failure to open.
    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
      this.properties = (HashMap<String, String>) in.readObject();
      this.maps = (HashMap<String, FlatFileKeyValueStorage<?, ?>>) in.readObject();
      in.close();
      for (Map.Entry<String, FlatFileKeyValueStorage<?, ?>> entry : maps.entrySet()) {
        entry.getValue().setFlushCallback(doFlush);
      }
    } catch (ClassNotFoundException e) {
      // ClassNotFoundException is NOT expected so re-throw it as a runtime exception.
      throw new RuntimeException(e);
    }
  }

  @Override
  public void create() throws IOException {
    this.properties = new HashMap<>();
    this.maps = new HashMap<>();
    // Write the file, for the first time, so that we can attempt to open it later, even if we don't write anything.
    this.doFlush.run();
  }

  @Override
  public void close() {
    doFlush.run();
  }

  @Override
  public Map<String, String> getProperties() {
    return this.properties;
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized <K, V> KeyValueStorage<K, V> getKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    // It appears as though we often don't create these, ahead-of-time.
    if (!maps.containsKey(name)) {
      FlatFileKeyValueStorage<K, V> storage = new FlatFileKeyValueStorage<>(doFlush);
      maps.put(name, storage);
    }
    return (KeyValueStorage<K, V>) maps.get(name);
  }

  @Override
  public synchronized <K, V> KeyValueStorage<K, V> createKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    if (!maps.containsKey(name)) {
      FlatFileKeyValueStorage<K, V> storage = new FlatFileKeyValueStorage<>(doFlush);
      maps.put(name, storage);
      return storage;
    }
    return (KeyValueStorage<K, V>)maps.get(name);
  }

  @Override
  public <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String name) {
    KeyValueStorage<K, V> storage =  (KeyValueStorage<K, V>)maps.get(name);
    maps.remove(name);
    return storage;
  }  

  @Override
  public Transaction begin() {
    return new Transaction() {

      @Override
      public void commit() {
        doFlush.run();
      }

      @Override
      public void abort() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
  }
  
  
}

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
import java.util.Map;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

import com.tc.util.Assert;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implements the simple key-value storage persistence system.  Note that any modifications made to the data stored within
 * this (via a returned key-value storage object) will invoke a flush of all data back to the disk.
 * NOTE:  the current implementation is NOT thread-safe so all consumers must be ensure serialized access to this object as
 * well as any key-value storage objects or properties maps it returns.
 */
public class FlatFilePersistentStorage implements IPersistentStorage {
  private final File store;
  private FlatFileProperties properties;
  private Map<String, FlatFileKeyValueStorage<?, ?>> maps;
  
  private final FlatFileWrite doFlush = new FlatFileWrite() {
    @Override
    public <T> T run(Callable<T> r) {
      T result = null;
      try {
        synchronized (store) {
            result = r.call();
            File temp = new File(store.getParentFile(), "temp_" + store.getName());
            FileOutputStream file = new FileOutputStream(temp);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(properties);
            out.writeObject(maps);
            out.flush();
            out.close();
            file.flush();
            file.close();
            Files.move(temp.toPath(), store.toPath(), 
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (Exception e) {
        // If something happened here, that is a serious bug so we need to assert.
        Assert.failure("Failure flushing FlatFileKeyValueStorage", e);
      }
      return result;
    }
  };
  
  public FlatFilePersistentStorage(File file) {
    this.store = file;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void open() throws IOException {
    // Note that we will fail out for FileNotFound and other IOExceptions since those are the checked kinds of failure to open.
    if (!store.exists()) {
      throw new IOException("not found");
    }
    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(store));
      this.properties = (FlatFileProperties)in.readObject();
      this.maps = (Map<String, FlatFileKeyValueStorage<?, ?>>) in.readObject();
      in.close();
      for (Map.Entry<String, FlatFileKeyValueStorage<?, ?>> entry : maps.entrySet()) {
        entry.getValue().setFlushCallback(doFlush);
      }
      this.properties.setWriter(doFlush);
    } catch (ClassNotFoundException e) {
      // ClassNotFoundException is NOT expected so re-throw it as a runtime exception.
      throw new RuntimeException(e);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public void create() throws IOException {
    this.properties = new FlatFileProperties(doFlush);
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
    synchronized (store) {
    if (!maps.containsKey(name)) {
      FlatFileKeyValueStorage<K, V> storage = new FlatFileKeyValueStorage<>(doFlush);
      maps.put(name, storage);
    }
    return (KeyValueStorage<K, V>) maps.get(name);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <K, V> KeyValueStorage<K, V> createKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    synchronized (store) {
    if (!maps.containsKey(name)) {
      FlatFileKeyValueStorage<K, V> storage = new FlatFileKeyValueStorage<>(doFlush);
      maps.put(name, storage);
      return storage;
    }
    return (KeyValueStorage<K, V>)maps.get(name);
    }
  }

  @Override
  public <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String name) {
    synchronized (store) {
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


}

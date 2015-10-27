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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import org.terracotta.persistence.KeyValueStorage;

/**
 * The key-value storage backed by a file on disk.  Note that any write to the storage is costly since it will result in
 * the entire backing flat file to be re-written to disk (this flush is aggressive, eager, and can only operate on the
 * entire file at once).
 */
public class FlatFileKeyValueStorage<K, V> implements KeyValueStorage<K, V>, Serializable {
  private final HashMap<K, V> storage;
  private transient Runnable doFlush;
  
  /**
   * This constructor is only used in the case of serialization.
   */
  public FlatFileKeyValueStorage() {
    this.storage = null;
    this.doFlush = null;
  }
  
  /**
   * This constructor is used when creating the storage for the first time, not from disk.
   */
  public FlatFileKeyValueStorage(Runnable doFlush) {
    this.storage = new HashMap<>();
    this.doFlush = doFlush;
  }
  
  /**
   * Provided for the deserialization case so that the flush method can be set after loading the object from disk.
   * Note that this will assert if called on an instance which already has a flush callback.
   */
  public void setFlushCallback(Runnable doFlush) {
    Assert.assertNull(this.doFlush);
    this.doFlush = doFlush;
  }
  
  @Override
  public void clear() {
    storage.clear();
    doFlush.run();
  }

  @Override
  public boolean containsKey(K key) {
    return storage.containsKey(key);
  }

  @Override
  public V get(K key) {
    return storage.get(key);
  }

  @Override
  public Set<K> keySet() {
    return storage.keySet();
  }

  @Override
  public void put(K key, V value) {
    storage.put(key, value);
    doFlush.run();
  }

  @Override
  public void put(K arg0, V arg1, byte arg2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(K key) {
    boolean didRemove = (null != storage.remove(key));
    doFlush.run();
    return didRemove;
  }

  @Override
  public void removeAll(Collection<K> keys) {
    for (K key : keys) {
      storage.remove(key);
    }
    doFlush.run();
  }

  @Override
  public long size() {
    return storage.size();
  }

  @Override
  public Collection<V> values() {
    return storage.values();
  }
}

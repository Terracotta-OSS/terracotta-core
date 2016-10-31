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
package org.terracotta.passthrough;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.IPlatformPersistence;
import org.terracotta.persistence.KeyValueStorage;


/**
 * NOTE:  This is largely based on EmulatedPersistentStorage (but substantially simplified), from terracotta-core.
 * XXX: Can be removed once IPersistentStorage is removed.
 */
public class PassthroughEmulatedPersistentStorage implements IPersistentStorage {
  private static final String FILE_NAME = "IPersistenceStorage.dat";

  private final IPlatformPersistence platformPersistence;
  private HashMap<String, HashMap<Object, Object>> perStoreData;
  

  public PassthroughEmulatedPersistentStorage(IPlatformPersistence platformPersistence) {
    Assert.assertTrue(null != platformPersistence);
    this.platformPersistence = platformPersistence;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public synchronized void open() throws IOException {
    this.perStoreData = (HashMap<String, HashMap<Object, Object>>) this.platformPersistence.loadDataElement(FILE_NAME);
    if (null == this.perStoreData) {
      // Treat this as a file not found - they should have used create.
      throw new IOException("not found");
    }
  }

  @Override
  public synchronized void create() throws IOException {
    this.perStoreData = new HashMap<String, HashMap<Object, Object>>();
    // We need to write the file even if empty since its existence is important.
    writeBack();
  }

  @Override
  public synchronized void close() {
    writeBack();
  }

  @Override
  public Map<String, String> getProperties() {
    // Not expected to be called in any current incarnation.
    Assert.unreachable();
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized <K, V> KeyValueStorage<K, V> getKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    if (!this.perStoreData.containsKey(name)) {
      this.perStoreData.put(name, new HashMap<Object, Object>());
    }
    return (KeyValueStorage<K, V>) new SerializableKeyValueStorage(name);
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <K, V> KeyValueStorage<K, V> createKeyValueStorage(String name, Class<K> keyClass, Class<V> valueClass) {
    KeyValueStorage<K, V> storage = null;
    if (!this.perStoreData.containsKey(name)) {
      this.perStoreData.put(name, new HashMap<Object, Object>());
      writeBack();
      storage = (KeyValueStorage<K, V>) new SerializableKeyValueStorage(name);
    }
    return storage;
  }

  @Override
  public synchronized <K, V> KeyValueStorage<K, V> destroyKeyValueStorage(String name) {
    KeyValueStorage<K, V> storage = null;
    if (this.perStoreData.containsKey(name)) {
      this.perStoreData.remove(name);
      writeBack();
    }
    return storage;
  }

  @Override
  public Transaction begin() {
    // Not used in any remaining cases.
    Assert.unreachable();
    return null;
  }


  private void writeBack() {
    try {
      this.platformPersistence.storeDataElement(FILE_NAME, this.perStoreData);
    } catch (IOException e) {
      Assert.unexpected(e);
    }
  }


  private synchronized Set<Object> local_keySet(String name) {
    return this.perStoreData.get(name).keySet();
  }

  private synchronized Collection<Object> local_values(String name) {
    return this.perStoreData.get(name).values();
  }

  private synchronized long local_size(String name) {
    return this.perStoreData.get(name).size();
  }

  private synchronized void local_put(String name, Object key, Object value) {
    this.perStoreData.get(name).put(key, value);
    writeBack();
  }

  private synchronized Object local_get(String name, Object key) {
    return this.perStoreData.get(name).get(key);
  }

  private synchronized boolean local_remove(String name, Object key) {
    boolean didRemove = (null != this.perStoreData.get(name).remove(key));
    writeBack();
    return didRemove;
  }

  private synchronized void local_removeAll(String name, Collection<Object> keys) {
    for (Object key : keys) {
      this.perStoreData.get(name).remove(key);
    }
    writeBack();
  }

  private synchronized boolean local_containsKey(String name, Object key) {
    return this.perStoreData.get(name).containsKey(key);
  }

  private synchronized void local_clear(String name) {
    this.perStoreData.get(name).clear();
    writeBack();
  }


  private class SerializableKeyValueStorage implements KeyValueStorage<Object, Object>, Serializable {
    private static final long serialVersionUID = 4609129381068644738L;
    
    private final String name;
    
    public SerializableKeyValueStorage(String name) {
      this.name = name;
    }
    
    @Override
    public Set<Object> keySet() {
      return PassthroughEmulatedPersistentStorage.this.local_keySet(this.name);
    }

    @Override
    public Collection<Object> values() {
      return PassthroughEmulatedPersistentStorage.this.local_values(this.name);
    }

    @Override
    public long size() {
      return PassthroughEmulatedPersistentStorage.this.local_size(this.name);
    }

    @Override
    public void put(Object key, Object value) {
      PassthroughEmulatedPersistentStorage.this.local_put(this.name, key, value);
    }

    @Override
    public void put(Object key, Object value, byte metadata) {
      // Not used.
      Assert.unreachable();
    }

    @Override
    public Object get(Object key) {
      return PassthroughEmulatedPersistentStorage.this.local_get(this.name, key);
    }

    @Override
    public boolean remove(Object key) {
      return PassthroughEmulatedPersistentStorage.this.local_remove(this.name, key);
    }

    @Override
    public void removeAll(Collection<Object> keys) {
      PassthroughEmulatedPersistentStorage.this.local_removeAll(this.name, keys);
    }

    @Override
    public boolean containsKey(Object key) {
      return PassthroughEmulatedPersistentStorage.this.local_containsKey(this.name, key);
    }

    @Override
    public void clear() {
      PassthroughEmulatedPersistentStorage.this.local_clear(this.name);
    }
  }
}

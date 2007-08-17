/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.memorydatastore.message.TCByteArrayKeyValuePair;
import com.tc.object.ObjectID;
import com.tc.util.Conversion;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MemoryStorePersistableMap implements Map {

  private MemoryDataStoreClient           db;
  private MemoryStoreCollectionsPersistor persistor;
  private int                             mapSize;
  private final long                      id;

  public MemoryStorePersistableMap(ObjectID id, MemoryStoreCollectionsPersistor persistor, MemoryDataStoreClient db) {
    this.id = id.toLong();
    this.persistor = persistor;
    this.db = db;
  }

  public int size() {
    return mapSize;
  }

  public boolean isEmpty() {
    return mapSize == 0;
  }

  public boolean containsKey(Object key) {
    return keySet().contains(key);
  }

  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  public Object get(Object key) {
    try {
      byte[] kb = persistor.serialize(id, key);
      Object value = persistor.deserialize(db.get(kb));
      return value;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public Object put(Object key, Object value) {
    try {
      byte[] kb = persistor.serialize(id, key);
      byte[] vb = persistor.serialize(value);
      Object returnVal = db.get(kb);
      db.put(kb, vb);
      if (returnVal != null) ++mapSize;
      return (returnVal);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Object remove(Object key) {
    try {
      byte[] kb = persistor.serialize(id, key);
      Object returnVal = db.get(kb);
      if (returnVal != null) {
        db.remove(kb);
        --mapSize;
      }
      return returnVal;
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void putAll(Map m) {
    for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public void clear() {
    try {
      db.removeAll(persistor.serialize(id));
      mapSize = 0;
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Set keySet() {
    Set keys = new HashSet();
    try {
      Collection all = db.getAll(persistor.serialize(id));
      for (Iterator i = all.iterator(); i.hasNext();) {
        TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
        keys.add(persistor.deserialize(Conversion.long2Bytes(id).length, pair.getKey()));
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
    return keys;
  }

  public Collection values() {
    Collection values = new HashSet();
    try {
      Collection all = db.getAll(persistor.serialize(id));
      for (Iterator i = all.iterator(); i.hasNext();) {
        TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
        values.add(persistor.deserialize(pair.getValue()));
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }

    return values;
  }

  public Set entrySet() {
    Map maps = new HashMap();
    try {
      Collection all = db.getAll(persistor.serialize(id));
      for (Iterator i = all.iterator(); i.hasNext();) {
        TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
        maps.put(persistor.deserialize(Conversion.long2Bytes(id).length, pair.getKey()), persistor.deserialize(pair.getValue()));
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }

    return maps.entrySet();
  }

  public void commit(MemoryStoreCollectionsPersistor persistor, MemoryDataStoreClient db) throws IOException {
  }

  public boolean equals(Object other) {
    if (!(other instanceof Map)) { return false; }
    Map that = (Map) other;
    if (that.size() != this.size()) { return false; }
    return entrySet().containsAll(that.entrySet());
  }

  public int hashCode() {
    int h = 0;
    for (Iterator i = entrySet().iterator(); i.hasNext();) {
      h += i.next().hashCode();
    }
    return h;
  }

  public String toString() {
    return "MemoryStorePersistableMap(" + id + ")={ size() = " + mapSize + " }";
  }

  public void load() {
    try {
      Collection all = db.getAll(persistor.serialize(id));
      mapSize = all.size();
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.memorydatastore.server;

import com.tc.memorydatastore.message.TCByteArrayKeyValuePair;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MemoryDataStore {
  private final Map        store          = new HashMap();

  public MemoryDataStore() {
    super();
  }

  public synchronized void put(String dataStoreName, byte[] key, byte[] value) {
    DataStore dataStore = getDataStore(dataStoreName);
    dataStore.put(key, value);
  }

  public synchronized byte[] get(String dataStoreName, byte[] key) {
    DataStore dataStore = getDataStore(dataStoreName);
    return dataStore.get(key);
  }

  public synchronized Collection getAll(String dataStoreName, byte[] key) {
    DataStore dataStore = getDataStore(dataStoreName);

    return dataStore.getAll(key);
  }

  public synchronized byte[] remove(String dataStoreName, byte[] key) {
    DataStore dataStore = getDataStore(dataStoreName);
    return dataStore.remove(key);
  }

  public synchronized int removeAll(String dataStoreName, byte[] key) {
    DataStore dataStore = getDataStore(dataStoreName);
    return dataStore.removeAll(key);
  }

  private DataStore getDataStore(String dataStoreName) {
    DataStore dataStore = (DataStore) this.store.get(dataStoreName);
    if (dataStore == null) {
      dataStore = new DataStore();
      this.store.put(dataStoreName, dataStore);
    }
    return dataStore;
  }

  private static class ByteArrayObjectWrapper implements Comparable {
    private byte[] value;

    public ByteArrayObjectWrapper(byte[] value) {
      this.value = value;
    }

    public int hashCode() {
      if (value == null) return 0;

      int result = 1;
      for (int i = 0; i < value.length; i++) {
        result = 31 * result + value[i];
      }

      return result;
    }

    public boolean equals(Object obj) {
      if (obj == null) { return false; }
      if (!(obj instanceof ByteArrayObjectWrapper)) { return false; }
      return Arrays.equals(this.value, ((ByteArrayObjectWrapper) obj).value);
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < value.length; i++) {
        sb.append(value[i]);
        sb.append(" ");
      }
      return sb.toString();
    }

    public int compareTo(Object obj) {
      if (obj == null) { throw new NullPointerException(); }
      Assert.assertTrue(obj instanceof ByteArrayObjectWrapper);

      ByteArrayObjectWrapper objWrapper = (ByteArrayObjectWrapper) obj;

      byte[] b = objWrapper.value;

      for (int i = 0; i < value.length; i++) {
        if (i >= b.length) {
          break;
        }
        if (value[i] == b[i]) {
          continue;
        }
        if (value[i] < b[i]) { return -1; }
        if (value[i] > b[i]) { return 1; }
      }

      if (value.length == b.length) { return 0; }
      if (value.length > b.length) { return 1; }
      return -1;
    }

    public boolean startsWith(ByteArrayObjectWrapper obj) {
      byte[] b = obj.value;
      if (value.length < b.length) { return false; }

      for (int i = 0; i < b.length; i++) {
        if (value[i] == b[i]) {
          continue;
        } else {
          return false;
        }
      }
      return true;
    }
  }

  private static class DataStore {
    private final TreeMap map = new TreeMap();

    public DataStore() {
      super();
    }

    public void put(byte[] key, byte[] value) {
      if (key.length <= 0) { return; }
      map.put(new ByteArrayObjectWrapper(key), value);
    }

    public byte[] get(byte[] key) {
      if (key.length <= 0) { return null; }

      return (byte[]) map.get(new ByteArrayObjectWrapper(key));
    }

    public Collection getAll(byte[] key) {
      if (key.length <= 0) {
        // return null;
      }

      // long startTime = System.currentTimeMillis();
      ByteArrayObjectWrapper wrappedKey = new ByteArrayObjectWrapper(key);
      Map subMap = map.tailMap(wrappedKey);
      // System.err.println("Num of potentials in getAll: " + subMap.size());

      Collection returnValues = new ArrayList();

      Set entrySet = subMap.entrySet();
      for (Iterator i = entrySet.iterator(); i.hasNext();) {
        Map.Entry entry = (Map.Entry) i.next();
        ByteArrayObjectWrapper k = (ByteArrayObjectWrapper) entry.getKey();
        byte[] v = (byte[]) entry.getValue();
        if (key.length == 0 || k.startsWith(wrappedKey)) {
          returnValues.add(new TCByteArrayKeyValuePair(k.value, v));
        } else {
          break;
        }
      }
      // System.err.println("Num to returns in getAll: " + returnValues.size());
      // long endTime = System.currentTimeMillis();
      // System.err.println("Time spent in getAll: " + (endTime - startTime) + " ms");

      return returnValues;
    }

    public byte[] remove(byte[] key) {
      if (key.length <= 0) { return null; }

      return (byte[]) map.remove(new ByteArrayObjectWrapper(key));
    }

    public int removeAll(byte[] key) {
      if (key.length <= 0) { return 0; }

      ByteArrayObjectWrapper wrappedKey = new ByteArrayObjectWrapper(key);
      Map subMap = map.tailMap(wrappedKey);
      // System.err.println("Num of potentials in getAll: " + subMap.size());

      int returnValue = 0;
      Set entrySet = subMap.entrySet();
      for (Iterator i = entrySet.iterator(); i.hasNext();) {
        Map.Entry entry = (Map.Entry) i.next();
        ByteArrayObjectWrapper k = (ByteArrayObjectWrapper) entry.getKey();
        if (k.startsWith(wrappedKey)) {
          i.remove();
          returnValue++;
        } else {
          break;
        }
      }

      // System.err.println("Num to remove: " + returnValue);

      return returnValue;
    }
  }

}

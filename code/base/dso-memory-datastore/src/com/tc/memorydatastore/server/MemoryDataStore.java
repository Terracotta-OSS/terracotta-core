/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  private final static int MAX_TRIE_LEVEL = 8;

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

  /*
   * private ByteTrie getDataStore(String dataStoreName) { ByteTrie dataStore =
   * (ByteTrie) this.store.get(dataStoreName); if (dataStore == null) {
   * dataStore = new ByteTrie(); this.store.put(dataStoreName, dataStore); }
   * return dataStore; }
   */

  private static class ByteArrayObjectWrapper implements Comparable {
    private byte[] value;

    public ByteArrayObjectWrapper(byte[] value) {
      this.value = value;
    }

    public int hashCode() {
      if (value == null)
        return 0;

      int result = 1;
      for (int i = 0; i < value.length; i++) {
        result = 31 * result + value[i];
      }

      return result;
    }

    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof ByteArrayObjectWrapper)) {
        return false;
      }
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
      if (obj == null) {
        throw new NullPointerException();
      }
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
        if (value[i] < b[i]) {
          return -1;
        }
        if (value[i] > b[i]) {
          return 1;
        }
      }

      if (value.length == b.length) {
        return 0;
      }
      if (value.length > b.length) {
        return 1;
      }
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
      if (key.length <= 0) {
        return;
      }
      map.put(new ByteArrayObjectWrapper(key), value);
    }

    public byte[] get(byte[] key) {
      if (key.length <= 0) {
        return null;
      }

      return (byte[]) map.get(new ByteArrayObjectWrapper(key));
    }

    public Collection getAll(byte[] key) {
      if (key.length <= 0) {
        return null;
      }

      //long startTime = System.currentTimeMillis();
      ByteArrayObjectWrapper wrappedKey = new ByteArrayObjectWrapper(key);
      Map subMap = map.tailMap(wrappedKey);
      //System.err.println("Num of potentials in getAll: " + subMap.size());

      Collection returnValues = new ArrayList();

      Set entrySet = subMap.entrySet();
      for (Iterator i = entrySet.iterator(); i.hasNext();) {
        Map.Entry entry = (Map.Entry) i.next();
        ByteArrayObjectWrapper k = (ByteArrayObjectWrapper) entry.getKey();
        byte[] v = (byte[]) entry.getValue();
        if (k.startsWith(wrappedKey)) {
          returnValues.add(new TCByteArrayKeyValuePair(k.value, v));
        } else {
          break;
        }
      }
      //System.err.println("Num to returns in getAll: " + returnValues.size());
      //long endTime = System.currentTimeMillis();
      //System.err.println("Time spent in getAll: " + (endTime - startTime) + " ms");

      return returnValues;
    }

    public byte[] remove(byte[] key) {
      if (key.length <= 0) {
        return null;
      }

      return (byte[]) map.remove(new ByteArrayObjectWrapper(key));
    }

    public int removeAll(byte[] key) {
      if (key.length <= 0) {
        return 0;
      }

      ByteArrayObjectWrapper wrappedKey = new ByteArrayObjectWrapper(key);
      Map subMap = map.tailMap(wrappedKey);
      //System.err.println("Num of potentials in getAll: " + subMap.size());

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

      //System.err.println("Num to remove: " + returnValue);

      return returnValue;
    }
  }

  private static class ByteTrie {
    private final static int     NUM_OF_ENTRIES = 256;
    private final ByteTrieNode[] head           = new ByteTrieNode[NUM_OF_ENTRIES];

    public ByteTrie() {
      super();
    }

    public void put(byte[] key, byte[] value) {
      if (key.length <= 0) {
        return;
      }

      ByteTrieNode node = getNode(key);
      node.put(key, value);
    }

    public byte[] get(byte[] key) {
      if (key.length <= 0) {
        return null;
      }

      ByteTrieNode node = getNode(key);
      return node.get(key);
    }

    public Collection getAll(byte[] key) {
      if (key.length <= 0) {
        return null;
      }

      ByteTrieNode node = getNode(key);
      return node.getAll();
    }

    public byte[] remove(byte[] key) {
      if (key.length <= 0) {
        return null;
      }

      ByteTrieNode node = getNode(key);
      return node.remove(key);
    }

    public int removeAll(byte[] key) {
      if (key.length <= 0) {
        return 0;
      }

      ByteTrieNode node = getNode(key);
      return node.removeAll(key);
    }

    public void dumpTrie() {
      for (int i = 0; i < head.length; i++) {
        if (head[i] != null) {
          System.err.println("head[" + i + "]: " + head[i]);
          head[i].dumpTrie(2);
        }
      }
    }

    private ByteTrieNode getNode(byte[] key) {
      ByteTrieNode node = getNode(head, key[0]);

      int level = (MAX_TRIE_LEVEL == -1 || key.length < MAX_TRIE_LEVEL) ? key.length : MAX_TRIE_LEVEL;
      for (int i = 1; i < level; i++) {
        node = getNode(node.next, key[i]);
      }
      return node;
    }

    private ByteTrieNode getNode(ByteTrieNode[] nodes, byte b) {
      ByteTrieNode node = nodes[b - 1];
      if (node == null) {
        node = new ByteTrieNode();
        nodes[b - 1] = node;
      }
      return node;
    }

    private static class ByteTrieNode {
      private final ByteTrieNode[] next        = new ByteTrieNode[NUM_OF_ENTRIES];
      private final Map            dataEntries = new HashMap();

      public void put(byte[] key, byte[] value) {
        dataEntries.put(new ByteArrayObjectWrapper(key), value);
      }

      public byte[] get(byte[] key) {
        return (byte[]) dataEntries.get(new ByteArrayObjectWrapper(key));
      }

      public Collection getAll() {
        Collection returnValue = new ArrayList();
        Set entrySet = dataEntries.entrySet();
        for (Iterator i = entrySet.iterator(); i.hasNext();) {
          Map.Entry entry = (Map.Entry) i.next();
          ByteArrayObjectWrapper key = (ByteArrayObjectWrapper) entry.getKey();
          TCByteArrayKeyValuePair keyValuePair = new TCByteArrayKeyValuePair(key.value, (byte[]) entry.getValue());
          returnValue.add(keyValuePair);
        }
        for (int i = 0; i < next.length; i++) {
          if (next[i] != null) {
            returnValue.addAll(next[i].getAll());
          }
        }
        return returnValue;
      }

      public byte[] remove(byte[] key) {
        return (byte[]) dataEntries.remove(new ByteArrayObjectWrapper(key));
      }

      public int removeAll(byte[] key) {
        int numOfRemove = getNumOfChilds();
        dataEntries.clear();
        for (int i = 0; i < next.length; i++) {
          next[i] = null;
        }
        return numOfRemove;
      }

      public void dumpTrie(int numOfSpaces) {
        printSpace(numOfSpaces);
        System.err.println("Size of data: " + dataEntries.size());
        Set keySet = dataEntries.keySet();
        for (Iterator i = keySet.iterator(); i.hasNext();) {
          Object key = i.next();
          printSpace(numOfSpaces);
          System.err.println("key: " + key);
        }

        for (int i = 0; i < next.length; i++) {
          if (next[i] != null) {
            printSpace(numOfSpaces);
            System.err.println("next[" + i + "]: " + next[i]);
            next[i].dumpTrie(numOfSpaces + 2);
          }
        }
      }

      private int getNumOfChilds() {
        int numOfChilds = 0;
        for (int i = 0; i < next.length; i++) {
          if (next[i] != null) {
            numOfChilds += next[i].getNumOfChilds();
          }
        }
        return dataEntries.size() + numOfChilds;
      }

      private void printSpace(int numOfSpaces) {
        for (int i = 0; i < numOfSpaces; i++) {
          System.err.print(' ');
        }
      }
    }

  }

}

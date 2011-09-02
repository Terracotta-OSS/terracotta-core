/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class TCPropertyStore {
  private final Map<Key, String> props = new LinkedHashMap<Key, String>();

  public synchronized boolean containsKey(String key) {
    return props.containsKey(new Key(key));
  }

  public synchronized void setProperty(String key, String value) {
    Key k = new Key(key);

    // do put and remove so that a replaced mapping will retain the new Key object
    props.remove(k);
    props.put(k, value.trim());
  }

  public synchronized String getProperty(String key) {
    return props.get(new Key(key));
  }

  public synchronized int size() {
    return props.size();
  }

  public synchronized String[] keysArray() {
    String[] keys = new String[props.size()];
    int index = 0;
    for (Key key : props.keySet()) {
      keys[index++] = key.getKey();
    }
    return keys;
  }

  public synchronized void load(InputStream in) throws IOException {
    new Properties() {
      @Override
      public synchronized Object put(Object key, Object value) {
        TCPropertyStore.this.setProperty((String) key, (String) value);
        return null;
      }
    }.load(in);
  }

  public synchronized void putAll(TCPropertyStore other) {
    for (String key : other.keysArray()) {
      setProperty(key, other.getProperty(key));
    }
  }

  private static class Key {
    private final String key;

    Key(String key) {
      this.key = key;
    }

    String getKey() {
      return key;
    }

    @Override
    public int hashCode() {
      return key.toLowerCase().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Key)) { return false; }

      Key other = (Key) obj;
      return key.equalsIgnoreCase(other.key);
    }
  }

}

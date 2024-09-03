/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class TCPropertyStore {
  private final Map<Key, String> props = new LinkedHashMap();

  public synchronized boolean containsKey(String key) {
    return props.containsKey(new Key(key));
  }

  public synchronized void setProperty(String key, String value) {
    Key k = new Key(key);

    // do put and remove so that a replaced mapping will retain the new Key object
    props.remove(k);
    // setting the value to null effectively unset the key
    if(value != null) {
      props.put(k, value.trim());
    }
  }

  public synchronized String getProperty(String key) {
    return props.get(new Key(key));
  }

  public synchronized int size() {
    return props.size();
  }
  
  public boolean isEmpty() {
    return props.isEmpty();
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

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class TCPropertyStore extends Properties {

  @Override
  public Object setProperty(String key, String value) {
    return super.setProperty(key.toLowerCase(), value);
  }

  @Override
  public Object put(Object key, Object value) {
    if (key instanceof String) {
      return super.put(((String) key).toLowerCase(), value);
    } else {
      throw new AssertionError("Only String keys allowed : " + key);
    }
  }

  @Override
  public void load(InputStream inStream) throws IOException {
    Properties incoming = new Properties();
    incoming.load(inStream);
    putAll(incoming);
  }

  @Override
  public void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
    Properties incoming = new Properties();
    incoming.loadFromXML(in);
    putAll(incoming);
  }

  @Override
  public Object get(Object key) {
    if (key instanceof String) {
      return super.get(((String) key).toLowerCase());
    } else {
      throw new AssertionError("Only String keys allowed : " + key);
    }
  }

  @Override
  public String getProperty(String key) {
    return super.getProperty(key.toLowerCase());
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    return super.getProperty(key.toLowerCase(), defaultValue);
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof String) {
      return super.containsKey(((String) key).toLowerCase());
    } else {
      throw new AssertionError("Only String keys allowed : " + key);
    }
  }

  @Override
  public Object remove(Object key) {
    if (key instanceof String) {
      return super.remove(((String) key).toLowerCase());
    } else {
      throw new AssertionError("Only String keys allowed : " + key);
    }
  }

  @Override
  public void putAll(Map<? extends Object, ? extends Object> t) {
    if (t instanceof TCPropertyStore) {
      super.putAll(t);
    } else {
      for (Entry<? extends Object, ? extends Object> e : t.entrySet()) {
        put(e.getKey(), e.getValue());
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TCPropertyStore) {
      return super.equals(o);
    } else if (o instanceof Map) {
      Map<Object, Object> m = (Map<Object, Object>) o;
      if (m.size() != size()) return false;

      try {
        for (Entry<Object, Object> e : m.entrySet()) {
          Object key = e.getKey();
          Object value = e.getValue();
          if (key instanceof String) {
            if (value == null) {
              if (!(get(key) == null && containsKey(key))) return false;
            } else {
              if (!value.equals(get(key))) return false;
            }
          } else {
            return false;
          }
        }
      } catch (ClassCastException unused) {
        return false;
      } catch (NullPointerException unused) {
        return false;
      }
      return true;
    } else {
      return false;
    }
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class TCPropertyStore implements Map<String, String> {

  private final Map<String, String> properties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

  public synchronized int size() {
    return properties.size();
  }

  public synchronized boolean isEmpty() {
    return properties.isEmpty();
  }

  public synchronized boolean containsKey(Object key) {
    return properties.containsKey(key);
  }

  public synchronized boolean containsValue(Object value) {
    return properties.containsValue(value);
  }

  public String get(Object key) {
    return properties.get(key);
  }

  public String put(String key, String value) {
    return properties.put(key, value);
  }

  public String remove(Object key) {
    return properties.remove(key);
  }

  public void putAll(Map<? extends String, ? extends String> t) {
    properties.putAll(t);
  }

  public void clear() {
    properties.clear();
  }

  public Set<String> keySet() {
    return properties.keySet();
  }

  public Collection<String> values() {
    return properties.values();
  }

  public Set<java.util.Map.Entry<String, String>> entrySet() {
    return properties.entrySet();
  }

  public String getProperty(String key) {
    return properties.get(key);
  }

  public String getProperty(String key, String defaultValue) {
    String val = getProperty(key);
    return (val == null) ? defaultValue : val;
  }

  public Object setProperty(String key, String value) {
    return properties.put(key, value);
  }

  private void putAllLooselyTyped(Map<?, ?> t) {
    for (Entry<?, ?> entry : t.entrySet()) {
      try {
        put((String) entry.getKey(), (String) entry.getValue());
      } catch (ClassCastException e) {
        throw (AssertionError) new AssertionError("Invalid type").initCause(e);
      }
    }
  }

  public void load(InputStream inStream) throws IOException {
    Properties incoming = new Properties();
    incoming.load(inStream);
    putAllLooselyTyped(incoming);
  }

  public void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
    Properties incoming = new Properties();
    incoming.loadFromXML(in);
    putAllLooselyTyped(incoming);
  }
}

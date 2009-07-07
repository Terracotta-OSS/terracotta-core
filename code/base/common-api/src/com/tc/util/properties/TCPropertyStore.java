/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.properties;

import com.tc.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class TCPropertyStore extends Properties {
  private final Map<String, String> propertyNameMap = new HashMap<String, String>();

  @Override
  public synchronized Object setProperty(String key, String value) {
    propertyNameMap.put(key.toLowerCase(), key);
    return super.setProperty(key, value);
  }

  @Override
  public synchronized Object put(Object key, Object value) {
    if (!(key instanceof String)) { throw new AssertionError("Only String keys allowed : " + key); }
    propertyNameMap.put(((String) key).toLowerCase(), (String) key);
    return super.put(key, value);
  }

  @Override
  public synchronized void load(InputStream inStream) throws IOException {
    super.load(inStream);
    Enumeration<?> keys = propertyNames();
    while (keys.hasMoreElements()) {
      String propertyName = (String) keys.nextElement();
      propertyNameMap.put(propertyName.toLowerCase(), propertyName);
    }
  }

  @Override
  public synchronized Object get(Object key) {
    if (!(key instanceof String)) { throw new AssertionError("Only String keys allowed : " + key); }
    String propertyName = propertyNameMap.get(((String) key).toLowerCase());
    return propertyName != null ? super.get(propertyName) : null;
  }

  @Override
  public String getProperty(String key) {
    String propertyName = propertyNameMap.get(key.toLowerCase());
    return propertyName != null ? super.getProperty(propertyName) : null;
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    String propertyName = propertyNameMap.get(key.toLowerCase());
    return propertyName != null ? super.getProperty(propertyName, defaultValue) : defaultValue;
  }

  @Override
  public synchronized boolean containsKey(Object key) {
    if (!(key instanceof String)) { throw new AssertionError("Only String keys allowed : " + key); }
    String propertyName = propertyNameMap.get(((String) key).toLowerCase());
    return propertyName != null ? super.containsKey(propertyName) : false;
  }

  @Override
  public synchronized Object remove(Object key) {
    if (!(key instanceof String)) { throw new AssertionError("Only String keys allowed : " + key); }
    String propertyName = propertyNameMap.get(((String) key).toLowerCase());
    return propertyName != null ? super.remove(propertyName) : super.remove(key);
  }

  @Override
  public synchronized void clear() {
    super.clear();
    propertyNameMap.clear();
  }

  @Override
  public synchronized Object clone() {
    throw new UnsupportedOperationException();
  }

  public synchronized void putAll(TCPropertyStore propStore) {
    Set<Object> keySet = propStore.keySet();
    for (Iterator iter = keySet.iterator(); iter.hasNext();) {
      String propertyName = (String) iter.next();
      propertyNameMap.put(propertyName.toLowerCase(), propertyName);
    }
    super.putAll(propStore);
  }

  @Override
  public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
    Assert.assertTrue(t instanceof TCPropertyStore);
    putAll(t);
  }

  // used in test
  public int keySize() {
    return propertyNameMap.size();
  }

}

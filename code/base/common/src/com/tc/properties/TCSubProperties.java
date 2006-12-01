/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.properties;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;

class TCSubProperties extends TCProperties {

  private final String category;
  private final TCProperties properties;

  public TCSubProperties(TCProperties properties, String category) {
    super(category);
    this.properties = properties;
    this.category = category;
  }

  public TCProperties getPropertiesFor(String category2) {
    return super.getPropertiesFor(getActualKey(category2));
  }

  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(getActualKey(key), defaultValue);
  }

  private String getActualKey(String key) {
    return category + "." + key;
  }

  public String getProperty(String key) {
    return properties.getProperty(getActualKey(key));
  }

  public void list(PrintStream out) {
    throw new UnsupportedOperationException();
  }

  public void list(PrintWriter out) {
    throw new UnsupportedOperationException();
  }

  public synchronized void load(InputStream inStream) {
    throw new UnsupportedOperationException();
  }

  public Enumeration propertyNames() {
    throw new UnsupportedOperationException();
  }

  public synchronized void save(OutputStream out, String header) {
    throw new UnsupportedOperationException();
  }

  public synchronized Object setProperty(String key, String value) {
    throw new UnsupportedOperationException();
  }

  public void store(OutputStream out, String header) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    return "TCSubProperties("+category+")"; 
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.test.schema;

public class GarbageCollectionConfigBuilder extends BaseConfigBuilder {

  private static final String[] ALL_PROPERTIES = new String[] { "enabled", "interval", "verbose" };

  public GarbageCollectionConfigBuilder() {
    super(3, ALL_PROPERTIES);
  }

  public void setGCEnabled(boolean data) {
    setProperty("enabled", data);
  }

  public void setGCEnabled(String data) {
    setProperty("enabled", data);
  }

  public void setGCVerbose(boolean data) {
    setProperty("verbose", data);
  }

  public void setGCVerbose(String data) {
    setProperty("verbose", data);
  }

  public void setGCInterval(int data) {
    setProperty("interval", data);
  }

  @Override
  public String toString() {
    String out = "";

    out += openElement("garbage-collection");

    for (String e : ALL_PROPERTIES) {
      out += element(e);
    }

    out += closeElement("garbage-collection");

    return out;
  }

}

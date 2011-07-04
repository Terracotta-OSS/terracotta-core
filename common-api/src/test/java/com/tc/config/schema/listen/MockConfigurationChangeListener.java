/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

/**
 * A mock {@link ConfigurationChangeListener}, for use in tests.
 */
public class MockConfigurationChangeListener implements ConfigurationChangeListener {

  private int       numConfigurationChangeds;
  private XmlObject lastOldConfig;
  private XmlObject lastNewConfig;

  public MockConfigurationChangeListener() {
    reset();
  }

  public void reset() {
    this.numConfigurationChangeds = 0;
    this.lastOldConfig = null;
    this.lastNewConfig = null;
  }

  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    ++this.numConfigurationChangeds;
    this.lastOldConfig = oldConfig;
    this.lastNewConfig = newConfig;
  }

  public XmlObject getLastNewConfig() {
    return lastNewConfig;
  }

  public XmlObject getLastOldConfig() {
    return lastOldConfig;
  }

  public int getNumConfigurationChangeds() {
    return numConfigurationChangeds;
  }

}

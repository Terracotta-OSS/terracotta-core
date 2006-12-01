/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.listen.ConfigurationChangeListener;

/**
 * A {@link MockConfigItem} that is also a {@link ConfigurationChangeListener}.
 */
public class MockListeningConfigItem extends MockConfigItem implements ConfigurationChangeListener {

  private int       numConfigurationChangeds;
  private XmlObject lastOldConfig;
  private XmlObject lastNewConfig;

  public void reset() {
    super.reset();

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

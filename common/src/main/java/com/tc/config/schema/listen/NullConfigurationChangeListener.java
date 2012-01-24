/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

/**
 * A {@link ConfigurationChangeListener} that doesn't do anything.
 */
public class NullConfigurationChangeListener implements ConfigurationChangeListener {

  private static final NullConfigurationChangeListener INSTANCE = new NullConfigurationChangeListener();
  
  public static NullConfigurationChangeListener getInstance() {
    return INSTANCE;
  }
  
  private NullConfigurationChangeListener() {
    // Nothing here.
  }
  
  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    // Nothing here.
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

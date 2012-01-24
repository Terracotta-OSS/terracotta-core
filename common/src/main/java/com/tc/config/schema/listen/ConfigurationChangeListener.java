/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

/**
 * Called when configuration is changed.
 */
public interface ConfigurationChangeListener {

  void configurationChanged(XmlObject oldConfig, XmlObject newConfig);
  
}

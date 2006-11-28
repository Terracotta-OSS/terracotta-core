/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

/**
 * Called when configuration is changed.
 */
public interface ConfigurationChangeListener {

  void configurationChanged(XmlObject oldConfig, XmlObject newConfig);
  
}

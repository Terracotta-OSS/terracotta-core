/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * An interface implemented by all config objects.
 */
public interface Config {

  void changesInItemIgnored(ConfigItem item);
  
  void changesInItemForbidden(ConfigItem item);
  
  XmlObject getBean();
  
}

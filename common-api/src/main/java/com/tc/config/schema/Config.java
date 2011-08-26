/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

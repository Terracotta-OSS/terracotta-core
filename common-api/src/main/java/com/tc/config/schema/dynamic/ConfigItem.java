/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;


/**
 * An object that knows how to return a particular piece of configuration data.
 */
public interface ConfigItem {
  
  Object getObject();
  
  void addListener(ConfigItemListener changeListener);

  void removeListener(ConfigItemListener changeListener);

}

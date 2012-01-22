/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

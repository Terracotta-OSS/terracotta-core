/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * An object that knows when the value a {@link ConfigItem} returns has changed.
 */
public interface ConfigItemListener {

  void valueChanged(Object oldValue, Object newValue);
  
}

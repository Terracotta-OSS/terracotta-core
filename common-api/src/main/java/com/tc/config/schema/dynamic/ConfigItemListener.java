/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * An object that knows when the value a {@link ConfigItem} returns has changed.
 */
public interface ConfigItemListener {

  void valueChanged(Object oldValue, Object newValue);
  
}

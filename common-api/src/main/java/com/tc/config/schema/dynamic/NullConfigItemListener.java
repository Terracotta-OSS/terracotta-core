/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A {@link ConfigItemListener} that does precisely nothing.
 */
public class NullConfigItemListener implements ConfigItemListener {

  private static final NullConfigItemListener INSTANCE = new NullConfigItemListener();
  
  public static NullConfigItemListener getInstance() {
    return INSTANCE;
  }
  
  private NullConfigItemListener() {
    // Nothing here.
  }
  
  public void valueChanged(Object oldValue, Object newValue) {
    // Nothing here.
  }

}

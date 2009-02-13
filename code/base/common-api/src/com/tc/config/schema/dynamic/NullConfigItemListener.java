/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

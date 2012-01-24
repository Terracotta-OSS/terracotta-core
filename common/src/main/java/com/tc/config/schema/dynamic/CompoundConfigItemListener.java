/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ConfigItemListener} that simply delegates to others.
 */
public class CompoundConfigItemListener implements ConfigItemListener {

  private final Set listeners;

  public CompoundConfigItemListener() {
    this.listeners = new HashSet();
  }

  public synchronized void addListener(ConfigItemListener listener) {
    Assert.assertNotNull(listener);
    this.listeners.add(listener);
  }

  public synchronized void removeListener(ConfigItemListener listener) {
    Assert.assertNotNull(listener);
    this.listeners.remove(listener);
  }

  public void valueChanged(Object oldValue, Object newValue) {
    ConfigItemListener[] duplicate;

    synchronized (this) {
      duplicate = (ConfigItemListener[]) this.listeners.toArray(new ConfigItemListener[this.listeners.size()]);
    }

    for (int i = 0; i < duplicate.length; ++i) {
      duplicate[i].valueChanged(oldValue, newValue);
    }
  }

}

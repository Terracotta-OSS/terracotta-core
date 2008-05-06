/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A set of {@link ConfigurationChangeListener}s.
 */
public class ConfigurationChangeListenerSet implements ConfigurationChangeListener {

  // This must be declared as a HashSet, not just a Set, so that we can clone it (below).
  private final HashSet changeListeners;

  public ConfigurationChangeListenerSet() {
    this.changeListeners = new HashSet();
  }

  public synchronized void addListener(ConfigurationChangeListener listener) {
    Assert.assertNotNull(listener);
    this.changeListeners.add(listener);
  }

  public synchronized void removeListener(ConfigurationChangeListener listener) {
    Assert.assertNotNull(listener);
    this.changeListeners.remove(listener);
  }

  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    Set dup;

    synchronized (this) {
      dup = (Set) this.changeListeners.clone();
    }

    Iterator iter = dup.iterator();
    while (iter.hasNext()) {
      ((ConfigurationChangeListener) iter.next()).configurationChanged(oldConfig, newConfig);
    }
  }

}

/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
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

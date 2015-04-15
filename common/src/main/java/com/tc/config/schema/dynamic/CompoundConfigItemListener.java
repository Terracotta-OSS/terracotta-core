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

  @Override
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

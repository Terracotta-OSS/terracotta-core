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
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectInstanceMonitorImpl implements ObjectInstanceMonitor, ObjectInstanceMonitorMBean {

  private final Map<String, Integer> instanceCounts = new HashMap<String, Integer>();

  public ObjectInstanceMonitorImpl() {
    //
  }

  @Override
  public synchronized void instanceCreated(String type) {
    if (type == null) { throw new IllegalArgumentException(); }

    Integer value = instanceCounts.get(type);
    if (value == null) {
      instanceCounts.put(type, 1);
    } else {
      instanceCounts.put(type, value + 1);
    }

  }

  @Override
  public synchronized void instanceDestroyed(String type) {
    if (type == null) { throw new IllegalArgumentException(); }
    Integer value = instanceCounts.get(type);
    if (value == null) {
      throw new IllegalStateException("No count available for type " + type);
    } else {
      instanceCounts.put(type, value - 1);
      if (instanceCounts.get(type) <= 0) {
        instanceCounts.remove(type);
      }
    }

  }

  @Override
  public synchronized Map getInstanceCounts() {
    final Map rv = new HashMap();
    for (Entry<String, Integer> entry : instanceCounts.entrySet()) {
      rv.put(entry.getKey(), entry.getValue());
    }

    return rv;
  }
}

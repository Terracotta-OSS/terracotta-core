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

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

public class ObjectInstanceMonitorTest extends TestCase {

  public void test() {

    ObjectInstanceMonitor monitor = new ObjectInstanceMonitorImpl();
    assertEquals(Collections.EMPTY_MAP, monitor.getInstanceCounts());

    monitor.instanceCreated("timmy");
    monitor.instanceCreated("timmy");
    monitor.instanceCreated("timmy");

    Map counts;
    counts = monitor.getInstanceCounts();
    assertEquals(1, counts.size());
    assertEquals(new Integer(3), counts.get("timmy"));

    monitor.instanceCreated("timmy2");
    counts = monitor.getInstanceCounts();
    assertEquals(2, counts.size());
    assertEquals(new Integer(3), counts.get("timmy"));
    assertEquals(new Integer(1), counts.get("timmy2"));

    monitor.instanceDestroyed("timmy2");
    counts = monitor.getInstanceCounts();
    assertEquals(1, counts.size());
    assertEquals(new Integer(3), counts.get("timmy"));

    monitor.instanceDestroyed("timmy");
    counts = monitor.getInstanceCounts();
    assertEquals(1, counts.size());
    assertEquals(Integer.valueOf(2), counts.get("timmy"));

    try {
      monitor.instanceDestroyed("timmy2");
      fail();
    } catch (IllegalStateException ise) {
      // expected
    }

    try {
      monitor.instanceDestroyed("monitor has never seen this string before");
      fail();
    } catch (IllegalStateException ise) {
      // expected
    }

    try {
      monitor.instanceCreated(null);
      fail();
    } catch (IllegalArgumentException ise) {
      // expected
    }

    try {
      monitor.instanceDestroyed(null);
      fail();
    } catch (IllegalArgumentException ise) {
      // expected
    }

  }

}

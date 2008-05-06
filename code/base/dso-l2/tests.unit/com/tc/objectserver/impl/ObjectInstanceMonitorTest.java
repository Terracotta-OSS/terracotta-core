/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
    assertEquals(new Integer(2), counts.get("timmy"));

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

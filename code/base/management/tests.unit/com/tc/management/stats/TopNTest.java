/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.stats;

import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;

public class TopNTest extends TestCase {

  public void testTopN() throws Exception {
    TopN topN = new TopN(3);
    for (int pos = -10; pos < 10; ++pos) {
      topN.evaluate(new Integer(pos));
    }
    Iterator pos = topN.iterator();
    assertEquals(new Integer(9), pos.next());
    assertEquals(new Integer(8), pos.next());
    assertEquals(new Integer(7), pos.next());
    assertFalse(pos.hasNext());
  }

  public void testTopNWithComparator() throws Exception {
    TopN topN = new TopN(Collections.reverseOrder(), 3);
    for (int pos = -10; pos < 10; ++pos) {
      topN.evaluate(new Integer(pos));
    }
    Iterator pos = topN.iterator();
    assertEquals(new Integer(-10), pos.next());
    assertEquals(new Integer(-9), pos.next());
    assertEquals(new Integer(-8), pos.next());
    assertFalse(pos.hasNext());
  }

}

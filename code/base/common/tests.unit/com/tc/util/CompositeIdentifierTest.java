/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.apache.commons.lang.ClassUtils;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class CompositeIdentifierTest extends TestCase {

  public void testOrderMatters() {
    AbstractIdentifier id1 = new IDType1(1);
    AbstractIdentifier id2 = new IDType2(1);

    CompositeIdentifier cid1 = new CompositeIdentifier(new AbstractIdentifier[] { id1, id2 });
    CompositeIdentifier cid2 = new CompositeIdentifier(new AbstractIdentifier[] { id2, id1 });

    assertFalse(cid1.equals(cid2));
    assertFalse(cid2.equals(cid1));
  }

  public void testContains() {
    AbstractIdentifier id1 = new IDType1(1);
    AbstractIdentifier id2 = new IDType2(1);

    CompositeIdentifier cid1 = new CompositeIdentifier(new AbstractIdentifier[] { id1, id2 });
    assertTrue(cid1.contains(id1));
    assertTrue(cid1.contains(id2));

    assertFalse(cid1.contains(new IDType1(2)));
  }

  public void testBasic() {
    Map map = new HashMap();

    AbstractIdentifier id1 = new IDType1(1);
    AbstractIdentifier id2 = new IDType2(1);

    CompositeIdentifier cid1 = new CompositeIdentifier(new AbstractIdentifier[] { id1, id2 });
    CompositeIdentifier cid2 = new CompositeIdentifier(new AbstractIdentifier[] { id1, id1 });

    map.put(cid1, "yo");

    assertEquals("yo", map.get(cid1));
    assertFalse(map.containsKey(cid2));
  }

  public void testToString() {
    AbstractIdentifier id1 = new IDType1(1);
    AbstractIdentifier id2 = new IDType2(1);

    CompositeIdentifier cid1 = new CompositeIdentifier(new AbstractIdentifier[] { id1, id2 });

    System.out.println(cid1);
  }

  public static class IDType1 extends AbstractIdentifier {
    public IDType1(long id) {
      super(id);
    }

    public String getIdentifierType() {
      return ClassUtils.getShortClassName(getClass());
    }
  }

  public static class IDType2 extends AbstractIdentifier {
    public IDType2(long id) {
      super(id);
    }

    public String getIdentifierType() {
      return ClassUtils.getShortClassName(getClass());
    }
  }

}

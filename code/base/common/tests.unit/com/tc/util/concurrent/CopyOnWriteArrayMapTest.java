/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class CopyOnWriteArrayMapTest extends TestCase {

  public void testBasic() throws Exception {
    CopyOnWriteArrayMap cam = new CopyOnWriteArrayMap();
    ArrayList al = new ArrayList();
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test put new key
    String s1 = "Hello there";
    al.add(s1);
    cam.put(s1, s1);
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test put new key
    String s2 = "Hello back";
    al.add(s2);
    cam.put(new Integer(10), s2);
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test put old key
    String s3 = "Hello Saro";
    al.remove(1);
    al.add(s3);
    cam.put(new Integer(10), s3);
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test remap
    cam.put(new Integer(10), s3);
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test putall
    Map m = new HashMap();
    m.put(new Long(9), new Float(9.1));
    m.put(new Long(19), new Float(91.1));
    m.put(new Long(191), new Float(191.1));
    al.addAll(m.values());
    cam.putAll(m);
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test non-existent key removal
    cam.remove("uv rays");
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test existent key removal
    al.remove(1);
    cam.remove(new Integer(10));
    assertArrayEquals(al.toArray(), cam.valuesArray());
    al.remove(al.size() - 1);
    cam.remove(new Long(191));
    assertArrayEquals(al.toArray(), cam.valuesArray());

    // test clear
    al.clear();
    cam.clear();
    assertArrayEquals(al.toArray(), cam.valuesArray());

  }

  private void assertArrayEquals(Object[] a1, Object[] a2) {
    print("a1", a1);
    print("a2", a2);
    if (a1 == null) {
      assertNull(a2);
      return;
    } else {
      assertNotNull(a2);
    }
    assertEquals(a1.length, a2.length);
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], a2[i]);
    }
    System.err.println("EQUAL");
  }

  private void print(String name, Object[] a) {
    System.err.print(name + " : ");
    if (a == null) {
      System.err.println("null");
      return;
    }
    for (int i = 0; i < a.length; i++) {
      System.err.print(a[i]);
      if (i < a.length - 1) {
        System.err.print(", ");
      }
    }
    System.err.println("");
  }

}

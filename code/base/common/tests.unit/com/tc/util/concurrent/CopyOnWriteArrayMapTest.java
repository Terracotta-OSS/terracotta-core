/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;

public class CopyOnWriteArrayMapTest extends TestCase {

  public void testBasic() throws Exception {
    CopyOnWriteArrayMap cam = new CopyOnWriteArrayMap();
    ArrayList al = new ArrayList();
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test put new key
    String s1 = "Hello there";
    al.add(s1);
    cam.put(s1, s1);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test put new key
    String s2 = "Hello back";
    al.add(s2);
    cam.put(new Integer(10), s2);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test put old key
    String s3 = "Hello Saro";
    al.remove(1);
    al.add(s3);
    cam.put(new Integer(10), s3);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test remap
    cam.put(new Integer(10), s3);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test putall
    Map m = new LinkedHashMap();
    m.put(new Long(9), new Float(9.1));
    m.put(new Long(19), new Float(91.1));
    m.put(new Long(191), new Float(191.1));
    al.addAll(m.values());
    cam.putAll(m);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test non-existent key removal
    cam.remove("uv rays");
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test existent key removal
    al.remove(1);
    cam.remove(new Integer(10));
    assertArrayEquals(al.toArray(), cam.values().toArray());
    al.remove(al.size() - 1);
    cam.remove(new Long(191));
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test clear
    al.clear();
    cam.clear();
    assertArrayEquals(al.toArray(), cam.values().toArray());

  }

  public void testSameValueMappedTo2Keys() throws Exception {
    CopyOnWriteArrayMap cam = new CopyOnWriteArrayMap();
    ArrayList al = new ArrayList();
    assertArrayEquals(al.toArray(), cam.values().toArray());

    Integer val = new Integer(0);
    for (int i = 0; i < 10;) {
      cam.put(Integer.toString(i), val);
      al.add(val);
      assertArrayEquals(al.toArray(), cam.values().toArray());
      if (++i % 2 == 0) {
        val = new Integer(i / 2);
      }
    }

    // remove
    for (int i = 0; i < 10; i++) {
      Integer val1 = new Integer(i / 2);
      Integer val2 = (Integer) cam.remove(Integer.toString(i));
      assertEquals(val1, val2);
      al.remove(val2);
      assertArrayEquals(al.toArray(), cam.values().toArray());
    }

  }
  
  public void testBasicEntrySet() throws Exception {
    CopyOnWriteArrayMap cam = new CopyOnWriteArrayMap();
    cam.put(new Integer(10), "String value 10");
    cam.put(new Integer(20), "String value 20");
    cam.put(new Integer(30), "String value 30");
    cam.put(new Integer(40), "String value 40");
   
    System.err.println(" Printing cam : " + cam);
    
    Map newMap = new HashMap(cam);
    assertEquals(cam, newMap);
    for (Iterator i = cam.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      System.err.println("Trying to remap : should throw an exception : " + e);
      try {
      e.setValue("Hey Jude");
      } catch(UnsupportedOperationException ue) {
        System.err.println("Caught exception as expected ;)");
      }
      System.err.println("Trying to trying to remove using the iterator : should throw an exception ");
      try {
        i.remove();
      } catch(UnsupportedOperationException ue) {
        System.err.println("Caught exception as expected ;)");
      }
    }
  }
  
  public void testBasicKeySet() throws Exception {
    CopyOnWriteArrayMap cam = new CopyOnWriteArrayMap();
    cam.put(new Integer(10), "String value 10");
    cam.put(new Integer(20), "String value 20");
    cam.put(new Integer(30), "String value 30");
    cam.put(new Integer(40), "String value 40");
    
    Set expectedKeys = new HashSet();
    expectedKeys.add(new Integer(10));
    expectedKeys.add(new Integer(20));
    expectedKeys.add(new Integer(30));
    expectedKeys.add(new Integer(40));
    
    Set keys = cam.keySet();
    assertEquals(expectedKeys, keys);
    
    int count = expectedKeys.size();
    for (Iterator i = keys.iterator(); i.hasNext();) {
      Object key = i.next();
      assertTrue(expectedKeys.contains(key));
      count--;
    }
    assertEquals(0, count);
  }
  
  public void testTypedArrayFactory() throws Exception {
    CopyOnWriteArrayMap cam = new CopyOnWriteArrayMap(new CopyOnWriteArrayMap.TypedArrayFactory() {
      public Object[] createTypedArray(int size) {
        return new String[size];
      }
    });
    cam.put(new Long(99), "hey");
    cam.put(new Long(999), "you");
    cam.put(new Long(9999), "jude");
    
    String values[] = (String[]) cam.valuesToArray();
    assertEquals(3, values.length);
    Set s = new HashSet(Arrays.asList(values));
    assertTrue(s.remove("hey"));
    assertTrue(s.remove("you"));
    assertTrue(s.remove("jude"));
    assertTrue(s.isEmpty());
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

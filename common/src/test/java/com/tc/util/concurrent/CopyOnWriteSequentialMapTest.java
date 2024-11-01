/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.concurrent;

import com.tc.util.concurrent.CopyOnWriteSequentialMap.TypedArrayFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.TestCase;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class CopyOnWriteSequentialMapTest extends TestCase {

  public void testBasic() throws Exception {
    CopyOnWriteSequentialMap cam = new CopyOnWriteSequentialMap();
    ArrayList al = new ArrayList();
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test put new key
    String s1 = "Hello there";
    al.add(s1);
    cam.put(s1, s1);
    assertArrayEquals(al.toArray(), cam.values().toArray(new Object[cam.size()]));

    // test put new key
    String s2 = "Hello back";
    al.add(s2);
    cam.put(Integer.valueOf(10), s2);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test put old key
    String s3 = "Hello Saro";
    al.remove(1);
    al.add(s3);
    cam.put(Integer.valueOf(10), s3);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test remap
    cam.put(Integer.valueOf(10), s3);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test putall
    Map m = new LinkedHashMap();
    m.put(Long.valueOf(9), 9.1f);
    m.put(Long.valueOf(19), 91.1f);
    m.put(Long.valueOf(191), 191.1f);
    al.addAll(m.values());
    cam.putAll(m);
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test non-existent key removal
    cam.remove("uv rays");
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test existent key removal
    al.remove(1);
    cam.remove(Integer.valueOf(10));
    assertArrayEquals(al.toArray(), cam.values().toArray());
    al.remove(al.size() - 1);
    cam.remove(Long.valueOf(191));
    assertArrayEquals(al.toArray(), cam.values().toArray());

    // test clear
    al.clear();
    cam.clear();
    assertArrayEquals(al.toArray(), cam.values().toArray());

  }

  public void testSameValueMappedTo2Keys() throws Exception {
    CopyOnWriteSequentialMap cam = new CopyOnWriteSequentialMap();
    ArrayList al = new ArrayList();
    assertArrayEquals(al.toArray(), cam.values().toArray());

    Integer val = Integer.valueOf(0);
    for (int i = 0; i < 10;) {
      cam.put(Integer.toString(i), val);
      al.add(val);
      assertArrayEquals(al.toArray(), cam.values().toArray());
      if (++i % 2 == 0) {
        val = Integer.valueOf(i / 2);
      }
    }

    // remove
    for (int i = 0; i < 10; i++) {
      Integer val1 = Integer.valueOf(i / 2);
      Integer val2 = (Integer) cam.remove(Integer.toString(i));
      assertEquals(val1, val2);
      al.remove(val2);
      assertArrayEquals(al.toArray(), cam.values().toArray());
    }

  }

  public void testBasicEntrySet() throws Exception {
    CopyOnWriteSequentialMap cam = new CopyOnWriteSequentialMap();
    cam.put(Integer.valueOf(10), "String value 10");
    cam.put(Integer.valueOf(20), "String value 20");
    cam.put(Integer.valueOf(30), "String value 30");
    cam.put(Integer.valueOf(40), "String value 40");

    System.err.println(" Printing cam : " + cam);

    Map newMap = new HashMap(cam);
    assertEquals(cam, newMap);
    for (Iterator i = cam.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      System.err.println("Trying to remap : should throw an exception : " + e);
      try {
        e.setValue("Hey Jude");
      } catch (UnsupportedOperationException ue) {
        System.err.println("Caught exception as expected ;)");
      }
      System.err.println("Trying to trying to remove using the iterator : should throw an exception ");
      try {
        i.remove();
      } catch (UnsupportedOperationException ue) {
        System.err.println("Caught exception as expected ;)");
      }
    }
  }

  public void testBasicKeySet() throws Exception {
    CopyOnWriteSequentialMap cam = new CopyOnWriteSequentialMap();
    cam.put(Integer.valueOf(10), "String value 10");
    cam.put(Integer.valueOf(20), "String value 20");
    cam.put(Integer.valueOf(30), "String value 30");
    cam.put(Integer.valueOf(40), "String value 40");

    Set expectedKeys = new HashSet();
    expectedKeys.add(Integer.valueOf(10));
    expectedKeys.add(Integer.valueOf(20));
    expectedKeys.add(Integer.valueOf(30));
    expectedKeys.add(Integer.valueOf(40));

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
    CopyOnWriteSequentialMap<Long, String> cam = new CopyOnWriteSequentialMap<Long, String>(new TypedArrayFactory() {

      @Override
      public String[] createTypedArray(int size) {
        return new String[size];
      }
    });
    cam.put(Long.valueOf(99), "hey");
    cam.put(Long.valueOf(999), "you");
    cam.put(Long.valueOf(9999), "jude");

    String values[] = cam.valuesToArray();
    assertEquals(3, values.length);
    Set s = new HashSet(Arrays.asList(values));
    assertTrue(s.remove("hey"));
    assertTrue(s.remove("you"));
    assertTrue(s.remove("jude"));
    assertTrue(s.isEmpty());
  }

  public void testConcurrentMod() {
    CopyOnWriteSequentialMap<Long, String> cam = new CopyOnWriteSequentialMap<Long, String>();
    cam.put(1l, "abc");
    cam.put(2l, "fff");
    Iterator<Long> keyItr = cam.keySet().iterator();
    assertEquals(Long.valueOf(1), keyItr.next());
    cam.put(120l, "def");
    assertEquals("fff", cam.get(keyItr.next()));
    assertFalse(keyItr.hasNext()); // no more elements in *iterated* snapshot
    assertEquals("def", cam.get(120l));
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

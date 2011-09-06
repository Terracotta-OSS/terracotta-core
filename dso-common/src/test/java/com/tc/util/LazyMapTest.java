/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;
import com.tc.util.LazyMap.LazyHashMap;
import com.tc.util.LazyMap.LazyLinkedHashMap;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LazyMapTest extends TCTestCase {

  public void testLazyHashMap() {
    LazyHashMap<ObjectID, ObjectID> map = new LazyHashMap<ObjectID, ObjectID>();
    lazyMapTest(map);
  }

  public void testLazyLinkedHashMap() {
    LazyLinkedHashMap<ObjectID, ObjectID> map = new LazyLinkedHashMap<ObjectID, ObjectID>();
    lazyMapTest(map);
  }

  private void lazyMapTest(LazyMap map) {
    ObjectID oid1 = new ObjectID(1);
    ObjectID oid2 = new ObjectID(2);
    ObjectID oid3 = new ObjectID(3);
    ObjectID oid4 = new ObjectID(4);

    Assert.assertTrue(0 == map.values().size());
    Assert.assertTrue(0 == map.keySet().size());
    Assert.assertTrue(0 == map.entrySet().size());

    map.put(oid1, oid2);

    Assert.assertTrue(1 == map.values().size());
    Assert.assertTrue(1 == map.keySet().size());
    Assert.assertTrue(1 == map.entrySet().size());

    Assert.assertEquals(oid2, map.get(oid1));

    // keySet
    Iterator<ObjectID> it = map.keySet().iterator();
    Assert.assertTrue(it.hasNext());
    Assert.assertEquals(oid1, it.next());
    Assert.assertFalse(it.hasNext());

    // values
    it = map.values().iterator();
    Assert.assertTrue(it.hasNext());
    Assert.assertEquals(oid2, it.next());
    Assert.assertFalse(it.hasNext());

    // entrySet
    Iterator<Map.Entry<ObjectID, ObjectID>> eit = map.entrySet().iterator();
    Assert.assertTrue(eit.hasNext());
    Map.Entry<ObjectID, ObjectID> entry = eit.next();
    Assert.assertEquals(oid1, entry.getKey());
    Assert.assertEquals(oid2, entry.getValue());
    Assert.assertFalse(it.hasNext());

    // remove by iterator
    it = map.values().iterator();
    Assert.assertTrue(it.hasNext());
    Assert.assertEquals(oid2, it.next());
    it.remove();
    Assert.assertTrue(0 == map.size());
    Assert.assertNull(map.get(oid1));

    // remove
    map.put(oid1, oid2);
    Assert.assertTrue(1 == map.size());
    Assert.assertEquals(oid2, map.remove(oid1));
    Assert.assertTrue(0 == map.size());

    // replace
    map.put(oid1, oid2);
    Assert.assertTrue(1 == map.size());
    map.put(oid1, oid3);
    Assert.assertTrue(1 == map.size());
    Assert.assertEquals(oid3, map.get(oid1));

    // add more than one entry
    map.put(oid3, oid4);
    Assert.assertTrue(2 == map.size());
    Assert.assertEquals(oid4, map.get(oid3));

    it = map.values().iterator();
    Assert.assertTrue(it.hasNext());
    it.next();
    it.remove();
    Assert.assertTrue(it.hasNext());
    it.next();
    it.remove();
    Assert.assertFalse(it.hasNext());
    Assert.assertTrue(0 == map.size());

    // test null key
    try {
      map.put(null, oid2);
      Assert.fail("null key shall not be allowed");
    } catch (NullPointerException e) {
      // expected exception
    }
    try {
      HashMap hmap = new HashMap();
      hmap.put(null, oid2);
      hmap.put(oid3, oid4);
      map.putAll(hmap);
      Assert.fail("null key shall not be allowed");
    } catch (NullPointerException e) {
      // expected exception
    }

    // remove
    {
      // Compare behaviors with HashMap
      HashMap map1 = new HashMap();
      map1.clear();
      map1.put(oid1, oid2);
      it = map1.values().iterator();
      try {
        it.remove();
        Assert.fail("Unexpected hashMap behavior");
      } catch (IllegalStateException e) {
        // expected
      }
      Assert.assertTrue(it.hasNext());
      Assert.assertEquals(oid2, it.next());
      it.remove();
      Assert.assertEquals(0, map1.size());

      // now with LazyMap
      map.clear();
      map.put(oid1, oid2);
      it = map.values().iterator();
      try {
        it.remove();
        Assert.fail("Iterator remove must be called after first next()");
      } catch (IllegalStateException e) {
        // expected
      }
      Assert.assertTrue(it.hasNext());
      Assert.assertEquals(oid2, it.next());
      it.remove();
      Assert.assertEquals(0, map.size());
    }

    // entry added/removed after Iterator
    map.clear();
    map.put(oid1, oid2);
    it = map.values().iterator();
    map.put(oid3, oid4);
    try {
      it.next();
      Assert.fail("Iterator shall throw ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
      // expected
    }

    map.clear();
    map.put(oid1, oid2);
    it = map.values().iterator();
    map.remove(oid1);
    try {
      it.next();
      Assert.fail("Iterator shall throw ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
      // expected
    }

    // update an entry of entrySet
    {
      Iterator<Map.Entry<ObjectID, ObjectID>> meIt;

      // compare with HashMap
      HashMap<ObjectID, ObjectID> map1 = new HashMap<ObjectID, ObjectID>();
      map1.put(oid1, oid2);
      meIt = map1.entrySet().iterator();
      Assert.assertTrue(meIt.hasNext());
      meIt.next().setValue(oid4);
      // entry write through
      Assert.assertEquals(oid4, map1.get(oid1));

      // now try LazyMap
      map.clear();
      map.put(oid1, oid2);
      meIt = map.entrySet().iterator();
      Assert.assertTrue(meIt.hasNext());
      meIt.next().setValue(oid4);
      // simulate what HashMap did, write through
      Assert.assertEquals(oid4, map.get(oid1));
    }

  }
}

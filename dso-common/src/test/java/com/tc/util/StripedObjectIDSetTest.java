/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import java.util.HashSet;
import java.util.Set;

public class StripedObjectIDSetTest extends TCTestCase {
  public void testBasic() throws Exception {
    HashSet<ObjectID> hashSet = new HashSet<ObjectID>();
    StripedObjectIDSet stripedObjectIdSet = new StripedObjectIDSet();

    // test ADD
    for (int i = -100; i < 100; i++) {
      Assert.assertTrue(hashSet.add(new ObjectID(i)));
      Assert.assertTrue(stripedObjectIdSet.add(new ObjectID(i)));
    }
    Assert.assertTrue(checkSame(stripedObjectIdSet, hashSet));

    // test contains
    for (int i = -100; i < 100; i++) {
      Assert.assertTrue(stripedObjectIdSet.contains(new ObjectID(i)));
    }

    // test contains All
    Assert.assertTrue(stripedObjectIdSet.containsAll(hashSet));

    // test size
    Assert.assertEquals(200, stripedObjectIdSet.size());

    for (int i = -100; i < 0; i++) {
      hashSet.remove(new ObjectID(i));
    }

    // test RetainAll
    Assert.assertTrue(stripedObjectIdSet.retainAll(hashSet));
    Assert.assertEquals(100, stripedObjectIdSet.size());

    // test remove
    Assert.assertTrue(stripedObjectIdSet.remove(new ObjectID(0)));
    Assert.assertFalse(stripedObjectIdSet.remove(new ObjectID(-1)));

    stripedObjectIdSet.add(new ObjectID(0));

    // test removeAll
    Assert.assertTrue(stripedObjectIdSet.removeAll(hashSet));
    Assert.assertEquals(0, stripedObjectIdSet.size());

    // test clear
    for (int i = -100; i < 100; i++) {
      Assert.assertTrue(stripedObjectIdSet.add(new ObjectID(i)));
    }
    Assert.assertEquals(200, stripedObjectIdSet.size());
    stripedObjectIdSet.clear();
    Assert.assertEquals(0, stripedObjectIdSet.size());
  }

  private boolean checkSame(Set<ObjectID> set1, Set<ObjectID> set2) {
    if (set1.size() != set2.size()) { return false; }

    return set1.containsAll(set2);
  }

  public void testUnsupportedMethods() throws Exception {
    StripedObjectIDSet stripedObjectIdSet = new StripedObjectIDSet();

    boolean exception = false;
    try {
      stripedObjectIdSet.first();
    } catch (UnsupportedOperationException e) {
      exception = true;
    }
    Assert.assertTrue(exception);

    exception = false;
    try {
      stripedObjectIdSet.last();
    } catch (UnsupportedOperationException e) {
      exception = true;
    }
    Assert.assertTrue(exception);

    exception = false;
    try {
      stripedObjectIdSet.headSet(new ObjectID(1));
    } catch (UnsupportedOperationException e) {
      exception = true;
    }
    Assert.assertTrue(exception);

    exception = false;
    try {
      stripedObjectIdSet.subSet(new ObjectID(1), new ObjectID(2));
    } catch (UnsupportedOperationException e) {
      exception = true;
    }
    Assert.assertTrue(exception);

    exception = false;
    try {
      stripedObjectIdSet.tailSet(new ObjectID(1));
    } catch (UnsupportedOperationException e) {
      exception = true;
    }
    Assert.assertTrue(exception);

    exception = false;
    try {
      stripedObjectIdSet.iterator();
    } catch (UnsupportedOperationException e) {
      exception = true;
    }
    Assert.assertTrue(exception);
  }

  public void testConcurrency() {
    HashSet<ObjectID> hashSet = new HashSet<ObjectID>();
    StripedObjectIDSet stripedObjectIdSet = new StripedObjectIDSet(16);

    for (int i = 0; i < 64 * 16; i = i + 16) {
      Assert.assertTrue(hashSet.add(new ObjectID(i)));
      Assert.assertTrue(stripedObjectIdSet.add(new ObjectID(i)));
    }
    Assert.assertTrue(checkSame(stripedObjectIdSet, hashSet));

    ObjectIDSet[] internalSets = stripedObjectIdSet.getObjectIDSets();

    int count = 0;
    for (int i = 0; i < internalSets.length; i++) {
      System.err.println("Internal Set " + i + " size: " + internalSets[i].size());
      if (internalSets[i].size() > 0) {
        count++;
      }
    }
    Assert.assertTrue(count == 16);
  }

  public void testToArray() {
    StripedObjectIDSet stripedObjectIdSet = new StripedObjectIDSet(16);

    for (int i = 0; i < 100; i++) {
      Assert.assertTrue(stripedObjectIdSet.add(new ObjectID(i)));
    }

    ObjectID[] o1 = new ObjectID[] {};
    o1 = stripedObjectIdSet.toArray(o1);

    Assert.assertEquals(100, o1.length);

    long prev = -1;
    for (int i = 0; i < 100; i++) {
      Assert.assertTrue(o1[i].toLong() > prev);
      prev = o1[i].toLong();
    }

    o1 = new ObjectID[101];
    o1 = stripedObjectIdSet.toArray(o1);

    Assert.assertEquals(101, o1.length);

    prev = -1;
    for (int i = 0; i < 100; i++) {
      Assert.assertTrue(o1[i].toLong() > prev);
      prev = o1[i].toLong();
    }

    Assert.assertNull(o1[100]);
  }
}

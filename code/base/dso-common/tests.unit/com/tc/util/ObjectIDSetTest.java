/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet.ObjectIDSetType;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ObjectIDSetTest extends TCTestCase {

  public void testContain() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final HashSet<ObjectID> hashSet = new HashSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testContain : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      hashSet.add(oid);
    }

    for (final ObjectID oid : hashSet) {
      Assert.assertTrue(bitSetBasedObjectIDSet.contains(oid));
      Assert.assertTrue(rangeBasedObjectIDSet.contains(oid));
    }
  }

  public void testIterator() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final TreeSet<ObjectID> treeSet = new TreeSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testIterator : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      treeSet.add(oid);
    }

    Assert.assertEquals(treeSet.size(), bitSetBasedObjectIDSet.size());
    Assert.assertEquals(treeSet.size(), rangeBasedObjectIDSet.size());

    final Iterator<ObjectID> tsIterator = treeSet.iterator();
    final Iterator<ObjectID> bitSetIterator = bitSetBasedObjectIDSet.iterator();
    final Iterator<ObjectID> rangeIterator = rangeBasedObjectIDSet.iterator();

    while (tsIterator.hasNext()) {
      final ObjectID oid = tsIterator.next();
      Assert.assertEquals(oid.toLong(), bitSetIterator.next().toLong());
      Assert.assertEquals(oid.toLong(), rangeIterator.next().toLong());
    }

    Assert.assertFalse(bitSetIterator.hasNext());
    Assert.assertFalse(rangeIterator.hasNext());
  }

  public void testNegativeIds() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final TreeSet<ObjectID> treeSet = new TreeSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testNegativeIds : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      treeSet.add(oid);
    }

    Assert.assertEquals(treeSet, rangeBasedObjectIDSet);
    Assert.assertEquals(treeSet, bitSetBasedObjectIDSet);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.remove(oid);
      rangeBasedObjectIDSet.remove(oid);
      treeSet.remove(oid);
    }

    Assert.assertEquals(treeSet, bitSetBasedObjectIDSet);
    Assert.assertEquals(treeSet, rangeBasedObjectIDSet);

    for (int i = 0; i < 1000000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      Assert.assertEquals(treeSet.contains(oid), bitSetBasedObjectIDSet.contains(oid));
    }
  }

  public void testFirstAndLast() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final TreeSet<ObjectID> treeSet = new TreeSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testFirstAndLast : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 10000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      treeSet.add(oid);
    }

    Assert.assertEquals(treeSet.first(), bitSetBasedObjectIDSet.first());
    Assert.assertEquals(treeSet.first(), rangeBasedObjectIDSet.first());

    Assert.assertEquals(treeSet.last(), bitSetBasedObjectIDSet.last());
    Assert.assertEquals(treeSet.last(), rangeBasedObjectIDSet.last());
  }

  public void testRemove() {
    ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    bitSetBasedObjectIDSet.add(new ObjectID(10));
    bitSetBasedObjectIDSet.add(new ObjectID(14));
    bitSetBasedObjectIDSet.add(new ObjectID(1));
    bitSetBasedObjectIDSet.add(new ObjectID(18));
    bitSetBasedObjectIDSet.add(new ObjectID(75));
    bitSetBasedObjectIDSet.add(new ObjectID(68));
    bitSetBasedObjectIDSet.add(new ObjectID(175));
    bitSetBasedObjectIDSet.add(new ObjectID(205));

    // data : [ Range(0,1000100010000000010) Range(64,100000010000)
    // Range(128,100000000000000000000000000000000000000000000000) Range(192,10000000000000)]
    // ids: 1, 10, 14, 18, 68, 75, 175. 205

    final Iterator<ObjectID> iterator = bitSetBasedObjectIDSet.iterator();
    iterateElements(iterator, 4);
    iterator.remove();
    Assert.assertEquals(68, iterator.next().toLong());

    iterateElements(iterator, 1);
    iterator.remove();
    Assert.assertEquals(175, iterator.next().toLong());
    iterator.remove();
    Assert.assertEquals(205, iterator.next().toLong());
    Assert.assertFalse(iterator.hasNext());

    // testing random removes

    bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangBasedOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final HashSet<ObjectID> hashSet = new HashSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testRemove : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 10000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangBasedOidSet.add(oid);
      hashSet.add(oid);
    }

    Assert.assertEquals(hashSet, bitSetBasedObjectIDSet);
    Assert.assertEquals(hashSet, rangBasedOidSet);

    for (int i = 0; i < 10000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.remove(oid);
      rangBasedOidSet.remove(oid);
      hashSet.remove(oid);
    }

    Assert.assertEquals(hashSet, bitSetBasedObjectIDSet);
    Assert.assertEquals(hashSet, rangBasedOidSet);
  }

  public void testPerformance() {
    ObjectIDSet bitSetBasedOidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    ObjectIDSet rangeBasedOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final HashSet<ObjectID> hashSet = new HashSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 800000; i++) {
      final long l = r.nextLong();
      final ObjectID id = new ObjectID(l);
      hashSet.add(id);
    }

    final long t1 = System.currentTimeMillis();
    for (final ObjectID objectID : hashSet) {
      bitSetBasedOidSet.add(objectID);
    }
    final long t2 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      rangeBasedOidSet.add(objectID);
    }
    final long t3 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      bitSetBasedOidSet.contains(objectID);
    }
    final long t4 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      rangeBasedOidSet.contains(objectID);
    }
    final long t5 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      bitSetBasedOidSet.remove(objectID);
    }
    final long t6 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      rangeBasedOidSet.remove(objectID);
    }
    final long t7 = System.currentTimeMillis();

    bitSetBasedOidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    rangeBasedOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);

    final long t8 = System.currentTimeMillis();
    bitSetBasedOidSet.addAll(hashSet);
    final long t9 = System.currentTimeMillis();
    rangeBasedOidSet.addAll(hashSet);
    final long t10 = System.currentTimeMillis();
    bitSetBasedOidSet.removeAll(hashSet);
    final long t11 = System.currentTimeMillis();
    rangeBasedOidSet.removeAll(hashSet);
    final long t12 = System.currentTimeMillis();

    System.out.println("comaprision, bitSetBased:rangeBased, add-> " + (t2 - t1) + ":" + (t3 - t2) + " contains->"
                       + (t4 - t3) + ":" + (t5 - t4) + " remove->" + (t6 - t5) + ":" + (t7 - t6));
    System.out.println("comaprision, bitSetBased:rangeBased, addAll-> " + (t9 - t8) + ":" + (t10 - t9) + " removeAll->"
                       + (t11 - t10) + ":" + (t12 - t11));
  }

  public Set createContinuousRangeBasedSet() {
    return new ObjectIDSet();
  }

  public Set create(final Collection c, final ObjectIDSetType objectIDSetType) {
    return new ObjectIDSet(c, objectIDSetType);
  }

  public void basicTest() {
    basicTest(100000, 100000, ObjectIDSetType.RANGE_BASED_SET);
    basicTest(500000, 100000, ObjectIDSetType.RANGE_BASED_SET);
    basicTest(100000, 1000000, ObjectIDSetType.RANGE_BASED_SET);

    basicTest(100000, 100000, ObjectIDSetType.BITSET_BASED_SET);
    basicTest(500000, 100000, ObjectIDSetType.BITSET_BASED_SET);
    basicTest(100000, 1000000, ObjectIDSetType.BITSET_BASED_SET);
  }

  public void testRemoveAll() {
    for (int i = 0; i < 10; i++) {
      timeAndTestRemoveAll();
    }

  }

  private void timeAndTestRemoveAll() {
    // HashSet expected = new HashSet();
    // HashSet big = new HashSet();
    // HashSet small = new HashSet();
    final TreeSet expected = new TreeSet();
    final TreeSet big = new TreeSet();
    final TreeSet small = new TreeSet();
    final ObjectIDSet rangeOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final ObjectIDSet bitSetOidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("RemoveALL TEST : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 1000000; i++) {
      // long l = r.nextLong();
      final long l = r.nextInt(55555555);
      final ObjectID id = new ObjectID(l);
      if (i % 2 == 0) {
        // 500,0000
        big.add(id);
      }
      if (i % 3 == 0) {
        // 333,000
        rangeOidSet.add(id);
        bitSetOidSet.add(id);
        expected.add(id);
      }
      if (i % 100 == 0) {
        small.add(id);
      }
    }

    final long t1 = System.currentTimeMillis();
    rangeOidSet.removeAll(small);
    final long t2 = System.currentTimeMillis();
    bitSetOidSet.removeAll(small);
    final long t3 = System.currentTimeMillis();
    expected.removeAll(small);
    final long t4 = System.currentTimeMillis();
    assertEquals(expected, rangeOidSet);
    assertEquals(expected, bitSetOidSet);

    final long t5 = System.currentTimeMillis();
    rangeOidSet.removeAll(big);
    final long t6 = System.currentTimeMillis();
    bitSetOidSet.removeAll(big);
    final long t7 = System.currentTimeMillis();
    expected.removeAll(big);
    final long t8 = System.currentTimeMillis();
    assertEquals(expected, rangeOidSet);
    assertEquals(expected, bitSetOidSet);

    System.err.println("Time taken for removeAll RangeObjectIDSet : BitSetObjectIDSet : HashSet : " + (t2 - t1) + " : "
                       + (t3 - t2) + " : " + (t4 - t3) + " millis  for small collection, " + (t6 - t5) + " : "
                       + (t7 - t6) + " : " + (t8 - t7) + " millis for large collection");
  }

  public void testSortedSetObjectIDSet() throws Exception {
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("SORTED TEST : Seed for Random is " + seed);
    final Random r = new Random(seed);
    final TreeSet ts = new TreeSet();
    final SortedSet oidsRangeBased = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final SortedSet oidsBitSetBased = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    for (int i = 0; i < 10000; i++) {
      final long l = r.nextLong();
      // if (l < 0) {
      // l = -l;
      // }
      final ObjectID id = new ObjectID(l);
      final boolean b1 = ts.add(id);
      final boolean b2 = oidsRangeBased.add(id);
      final boolean b3 = oidsBitSetBased.add(id);
      assertEquals(b1, b2);
      assertEquals(b1, b3);
      assertEquals(ts.size(), oidsRangeBased.size());
      assertEquals(ts.size(), oidsBitSetBased.size());
    }

    // verify sorted
    Iterator i = ts.iterator();
    for (final Iterator j = oidsRangeBased.iterator(); j.hasNext();) {
      final ObjectID oid1 = (ObjectID) i.next();
      final ObjectID oid2 = (ObjectID) j.next();
      assertEquals(oid1, oid2);
    }

    i = ts.iterator();
    for (final Iterator j = oidsBitSetBased.iterator(); j.hasNext();) {
      final ObjectID oid1 = (ObjectID) i.next();
      final ObjectID oid2 = (ObjectID) j.next();
      assertEquals(oid1, oid2);
    }
  }

  public void basicTest(final int distRange, final int iterationCount, final ObjectIDSetType objectIDSetType) {
    final long test_start = System.currentTimeMillis();
    final Set s = new HashSet();
    final Set small = new ObjectIDSet(objectIDSetType);
    final String cname = small.getClass().getName();
    System.err.println("Running tests for " + cname + " distRange = " + distRange + " iterationCount = "
                       + iterationCount);
    assertTrue(small.isEmpty());
    assertTrue(small.size() == 0);
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Seed for Random is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < iterationCount; i++) {
      final long l = r.nextInt(distRange);
      final ObjectID id = new ObjectID(l);
      s.add(id);
      small.add(id);
      assertEquals(s.size(), small.size());
    }
    final Iterator sit = small.iterator();
    final List all = new ArrayList();
    all.addAll(s);
    while (sit.hasNext()) {
      final ObjectID i = (ObjectID) sit.next();
      Assert.eval("FAILED:" + i.toString(), s.remove(i));
    }
    Assert.eval(s.size() == 0);

    // test retain all
    final Set odds = new HashSet();
    final Set evens = new HashSet();
    for (int i = 0; i < all.size(); i++) {
      if (i % 2 == 0) {
        evens.add(all.get(i));
      } else {
        odds.add(all.get(i));
      }
    }

    boolean b = small.retainAll(odds);
    assertTrue(b);
    Assert.assertEquals(odds.size(), small.size());
    assertEquals(odds, small);
    b = small.retainAll(evens);
    assertTrue(b);
    assertEquals(0, small.size());
    small.addAll(all); // back to original state

    // test new set creation (which uses cloning
    long start = System.currentTimeMillis();
    final Set copy = create(all, objectIDSetType);
    System.err.println("Time to add all IDs from a collection to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");
    start = System.currentTimeMillis();
    final Set clone = create(small, objectIDSetType);
    System.err.println("Time to add all IDs from an ObjectIDSet to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");

    Collections.shuffle(all);
    for (final Iterator i = all.iterator(); i.hasNext();) {
      final ObjectID rid = (ObjectID) i.next();
      Assert.eval(small.contains(rid));
      Assert.eval(clone.contains(rid));
      Assert.eval(copy.contains(rid));
      if (!small.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (small.contains(rid)) { throw new AssertionError(rid); }
      if (!clone.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (clone.contains(rid)) { throw new AssertionError(rid); }
      if (!copy.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (copy.contains(rid)) { throw new AssertionError(rid); }
    }
    for (final Iterator i = all.iterator(); i.hasNext();) {
      final ObjectID rid = (ObjectID) i.next();
      Assert.eval(!small.contains(rid));
      if (small.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (small.contains(rid)) { throw new AssertionError(rid); }
      if (clone.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (clone.contains(rid)) { throw new AssertionError(rid); }
      if (copy.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (copy.contains(rid)) { throw new AssertionError(rid); }
    }
    Assert.eval(s.size() == 0);
    Assert.eval(small.size() == 0);
    Assert.eval(copy.size() == 0);
    Assert.eval(clone.size() == 0);
    System.err.println("Time taken to run basic Test for " + small.getClass().getName() + " is "
                       + (System.currentTimeMillis() - test_start) + " ms");
  }

  public void testSerializationObjectIDSet2() throws Exception {
    for (int i = 0; i < 20; i++) {
      final Set s = createRandomSetOfObjectIDs();
      serializeAndVerify(s, ObjectIDSetType.RANGE_BASED_SET);
      serializeAndVerify(s, ObjectIDSetType.BITSET_BASED_SET);
    }
  }

  private void serializeAndVerify(final Set s, final ObjectIDSetType objectIDSetType) throws Exception {
    final ObjectIDSet org = new ObjectIDSet(s, objectIDSetType);
    assertEquals(s, org);

    final ObjectIDSet ser = serializeAndRead(org);
    assertEquals(s, ser);
    assertEquals(org, ser);
  }

  private ObjectIDSet serializeAndRead(final ObjectIDSet org) throws Exception {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    org.serializeTo(out);
    System.err.println("Written ObjectIDSet2 size : " + org.size());
    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    final ObjectIDSet oids = new ObjectIDSet();
    oids.deserializeFrom(in);
    System.err.println("Read  ObjectIDSet2 size : " + oids.size());
    return oids;
  }

  private Set createRandomSetOfObjectIDs() {
    final Set s = new HashSet();
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Random Set creation : Seed for Random is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < r.nextLong(); i++) {
      s.add(new ObjectID(r.nextLong()));
    }
    System.err.println("Created a set of size : " + s.size());
    return s;
  }

  public void testObjectIDSet() {
    basicTest();
  }

  public void testObjectIDSetDump() {
    final ObjectIDSet s1 = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final ObjectIDSet s2 = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    System.err.println(" toString() : " + s1);

    for (int i = 0; i < 100; i++) {
      s1.add(new ObjectID(i));
    }
    System.err.println(" toString() : " + s1);

    for (int i = 0; i < 100; i += 2) {
      s1.remove(new ObjectID(i));
    }
    System.err.println(" toString() : " + s1);

    System.err.println(" toString() : " + s2);

    for (int i = 0; i < 100; i++) {
      s2.add(new ObjectID(i));
    }
    System.err.println(" toString() : " + s2);

    for (int i = 0; i < 100; i += 2) {
      s2.remove(new ObjectID(i));
    }
    System.err.println(" toString() : " + s2);

  }

  public void testObjectIdSetConcurrentModification() {
    concurrentModificationTest(new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET));
    concurrentModificationTest(new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET));
  }

  private void concurrentModificationTest(final ObjectIDSet objIdSet) throws AssertionError {
    int num = 0;
    for (num = 0; num < 50; num++) {
      objIdSet.add(new ObjectID(num));
    }

    Iterator iterator = objIdSet.iterator();
    objIdSet.add(new ObjectID(num));
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (final ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

    iterator = objIdSet.iterator();
    objIdSet.remove(new ObjectID(0));
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (final ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

    iterator = objIdSet.iterator();
    objIdSet.clear();
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (final ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }
  }

  private long iterateElements(final Iterator iterator) throws ConcurrentModificationException {
    return iterateElements(iterator, -1);
  }

  private long iterateElements(final Iterator iterator, final long count) throws ConcurrentModificationException {
    long itrCount = 0;
    while ((iterator.hasNext()) && (count < 0 || itrCount < count)) {
      itrCount++;
      System.out.print(((ObjectID) iterator.next()).toLong() + ", ");
    }
    System.out.print("\n\n");
    return itrCount;
  }

  public void testObjectIDSetIteratorFullRemove() {
    oidSetIteratorFullRemoveTest(new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET));
    oidSetIteratorFullRemoveTest(new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET));
  }

  private void oidSetIteratorFullRemoveTest(final Set oidSet) {
    final Set all = new TreeSet();
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < 5000; i++) {
      final long l = r.nextLong();
      final ObjectID id = new ObjectID(l);
      all.add(id);
      oidSet.add(id);
    }

    Assert.assertEquals(all.size(), oidSet.size());
    for (final Iterator i = all.iterator(); i.hasNext();) {
      final ObjectID rid = (ObjectID) i.next();
      Assert.eval(oidSet.contains(rid));
      for (final Iterator j = oidSet.iterator(); j.hasNext();) {
        final ObjectID crid = (ObjectID) j.next();
        if (crid.equals(rid)) {
          j.remove();
          break;
        }
      }
    }
    Assert.assertEquals(oidSet.size(), 0);
  }

  public void testObjectIDSetIteratorSparseRemove() {
    oidSetIteratorSparseRemoveTest(new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET));
    oidSetIteratorSparseRemoveTest(new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET));
  }

  private void oidSetIteratorSparseRemoveTest(final Set oidSet) {
    // TreeSet<ObjectID> ts = new TreeSet<ObjectID>();
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < 1000; i++) {
      ObjectID id;
      do {
        final long l = r.nextLong();
        id = new ObjectID(l);
      } while (oidSet.contains(id));
      // ts.add(id);
      oidSet.add(id);
    }

    System.out.println(oidSet + "\n\n");
    // check if ObjectIDSet has been inited with 1000 elements
    Iterator oidSetIterator = oidSet.iterator();
    assertEquals(1000, iterateElements(oidSetIterator));

    long visitedCount = 0;
    long removedCount = 0;
    oidSetIterator = oidSet.iterator();

    // visit first 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100, visitedCount);

    // remove the 100th element
    oidSetIterator.remove();
    removedCount += 1;

    // visit next 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100 + 100, visitedCount);

    // remove the 200th element
    oidSetIterator.remove();
    removedCount += 1;

    // visit next 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100 + 100 + 100, visitedCount);

    // visit rest of the elements
    visitedCount += iterateElements(oidSetIterator);
    assertEquals(1000, visitedCount);

    // check the backing Set for removed elements
    oidSetIterator = oidSet.iterator();
    final long totalElements = iterateElements(oidSetIterator);
    assertEquals((visitedCount - removedCount), totalElements);
  }

  public void testObjectIDSetIteratorRemoveSpecailCases() {
    final List longList = new ArrayList();
    longList.add(new ObjectID(25));
    longList.add(new ObjectID(26));
    longList.add(new ObjectID(27));
    longList.add(new ObjectID(28));
    longList.add(new ObjectID(9));
    longList.add(new ObjectID(13));
    longList.add(new ObjectID(12));
    longList.add(new ObjectID(14));
    longList.add(new ObjectID(18));
    longList.add(new ObjectID(2));
    longList.add(new ObjectID(23));
    longList.add(new ObjectID(47));
    longList.add(new ObjectID(35));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(1));
    longList.add(new ObjectID(4));
    longList.add(new ObjectID(15));
    longList.add(new ObjectID(8));
    longList.add(new ObjectID(56));
    longList.add(new ObjectID(11));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(33));
    longList.add(new ObjectID(17));
    longList.add(new ObjectID(29));
    longList.add(new ObjectID(19));
    // Data : 1 2 4 8 9 10 11 12 13 14 15 17 18 19 23 25 26 27 28 29 33 35 47 56

    /**
     * ObjectIDSet { (oids:ranges) = 24:10 , compression ratio = 1.0 } [ Range(1,2) Range(4,4) Range(8,15) Range(17,19)
     * Range(23,23) Range(25,29) Range(33,33) Range(35,35) Range(47,47) Range(56,56)]
     */

    final int totalElements = longList.size() - 1;

    oidSetIteratorRemoveSpecialCasesTest(totalElements, new ObjectIDSet(longList, ObjectIDSetType.RANGE_BASED_SET));
    oidSetIteratorRemoveSpecialCasesTest(totalElements, new ObjectIDSet(longList, ObjectIDSetType.BITSET_BASED_SET));
  }

  private void oidSetIteratorRemoveSpecialCasesTest(final int totalElements, final Set objectIDSet)
      throws AssertionError {
    Iterator i = objectIDSet.iterator();
    assertEquals(totalElements, iterateElements(i));

    final List longSortList = new ArrayList();
    i = objectIDSet.iterator();
    while (i.hasNext()) {
      longSortList.add(i.next());
    }

    // remove first element in a range. eg: 8 from (8,15)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(8)) + 1, 9);
    objectIDSet.add(new ObjectID(8)); // get back to original state

    // remove last element in a range. eg: 19 from (17,19)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(19)) + 1, 23);
    objectIDSet.add(new ObjectID(19));

    // remove the only element in the range. eg: 33 from (33,33)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(33)) + 1, 35);
    objectIDSet.add(new ObjectID(33));

    // remove the least element
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(1)) + 1, 2);
    objectIDSet.add(new ObjectID(1));

    // remove the max element; element will be removed, but while going to next element, exception expected
    try {
      removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(56)) + 1, -99);
      throw new AssertionError("Expected to throw an exception");
    } catch (final NoSuchElementException noSE) {
      // expected
    } finally {
      objectIDSet.add(new ObjectID(56));
    }

    // remove the non existing element; exception expected
    try {
      removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(16)) + 1, -99);
      throw new AssertionError("Expected to throw an exception");
    } catch (final IllegalStateException ise) {
      // expected
    }

    i = objectIDSet.iterator();
    assertEquals(5, iterateElements(i, 5));
    objectIDSet.add(new ObjectID(99));
    try {
      assertEquals(5, iterateElements(i, 1));
      throw new AssertionError("Expected to throw an exception");
    } catch (final ConcurrentModificationException cme) {
      // expected
    } finally {
      objectIDSet.remove(new ObjectID(99));
    }
  }

  public void testAddAll() {

    final int SIZE_MILLION = 1000000;
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);

    // validate addAll
    addToReferencesRandom(bitSetBasedObjectIDSet, SIZE_MILLION);
    int randomSize = bitSetBasedObjectIDSet.size();

    final ObjectIDSet bitSetBasedObjectIDSet2 = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);

    long startTime = System.currentTimeMillis();
    bitSetBasedObjectIDSet2.addAll(bitSetBasedObjectIDSet);
    long addAllTime = System.currentTimeMillis() - startTime;

    System.out.println("bitSet.addAll random total time took: " + addAllTime + " ms. ");

    // validate addAll
    assertEquals(randomSize, bitSetBasedObjectIDSet2.size());

    for (final ObjectID id : bitSetBasedObjectIDSet) {
      assertTrue(bitSetBasedObjectIDSet2.contains(id));
    }

    // /do serial
    final ObjectIDSet bitSetBasedObjectIDSetSerial = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    addToReferencesSerial(bitSetBasedObjectIDSetSerial, SIZE_MILLION);

    assertEquals(SIZE_MILLION, bitSetBasedObjectIDSetSerial.size());

    startTime = System.currentTimeMillis();
    bitSetBasedObjectIDSet2.addAll(bitSetBasedObjectIDSetSerial);
    addAllTime = System.currentTimeMillis() - startTime;

    System.out.println("bitSet.addAll serial total time took: " + addAllTime + " ms. ");

    // validate addAll
    assertEquals(randomSize + SIZE_MILLION, bitSetBasedObjectIDSet2.size());

    for (final ObjectID id : bitSetBasedObjectIDSetSerial) {
      assertTrue(bitSetBasedObjectIDSet2.contains(id));
    }

    // RANGE SET
    // TODO:: Uncomment once Range set implements addAll
    /*
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    addToReferencesRandom(rangeBasedObjectIDSet, SIZE_MILLION);
    randomSize = rangeBasedObjectIDSet.size();

    final ObjectIDSet rangeBasedObjectIDSet2 = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);

    startTime = System.currentTimeMillis();
    rangeBasedObjectIDSet2.addAll(rangeBasedObjectIDSet);
    addAllTime = System.currentTimeMillis() - startTime;

    System.out.println("rangeSet.addAll random total time took: " + addAllTime + " ms. ");

    // validate addAll
    assertEquals(randomSize, rangeBasedObjectIDSet2.size());

    for (final ObjectID id : rangeBasedObjectIDSet) {
      assertTrue(rangeBasedObjectIDSet2.contains(id));
    }

    // do serial
    final ObjectIDSet rangeBasedObjectIDSetSerial = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    addToReferencesSerial(rangeBasedObjectIDSetSerial, SIZE_MILLION);

    assertEquals(SIZE_MILLION, rangeBasedObjectIDSetSerial.size());

    startTime = System.currentTimeMillis();
    rangeBasedObjectIDSet2.addAll(rangeBasedObjectIDSetSerial);
    addAllTime = System.currentTimeMillis() - startTime;

    System.out.println("rangeSet.addAll serial total time took: " + addAllTime + " ms. ");

    // validate addAll
    assertEquals(randomSize + SIZE_MILLION, rangeBasedObjectIDSet2.size());

    for (final ObjectID id : rangeBasedObjectIDSetSerial) {
      assertTrue(rangeBasedObjectIDSet2.contains(id));
    }
   

    //now lets add to serial, and see if the random set exist in it
    rangeBasedObjectIDSetSerial.addAll(rangeBasedObjectIDSet);
    
    for(ObjectID id : rangeBasedObjectIDSet) {
      assertTrue(rangeBasedObjectIDSetSerial.contains(id));
    }
    
     */
  }
  
  public void testAddAllPerformance() {
    
    final int SIZE_10_MILLION = 10000000;
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet bitSetBasedObjectIDSet2 = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    addToReferencesRandom(bitSetBasedObjectIDSet, SIZE_10_MILLION);
    addToReferencesSerial(bitSetBasedObjectIDSet2, SIZE_10_MILLION);
    int bitSize = bitSetBasedObjectIDSet.size();
    int bitSize2 = bitSetBasedObjectIDSet2.size();
    
    long startTime = System.currentTimeMillis();
    bitSetBasedObjectIDSet.addAll(bitSetBasedObjectIDSet2);   
    long addAllTime = System.currentTimeMillis() - startTime;
    
    System.out.println("bitSet.addAll random total time took: " + addAllTime + " ms. ");
    assertEquals(bitSize + bitSize2, bitSetBasedObjectIDSet.size());
    
  
    
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet2 = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    addToReferencesRandom(rangeBasedObjectIDSet, SIZE_10_MILLION);
    addToReferencesSerial(rangeBasedObjectIDSet2, SIZE_10_MILLION);
    
    int rangeSize = rangeBasedObjectIDSet.size();
    int rangeSize2 = rangeBasedObjectIDSet2.size();
    
    startTime = System.currentTimeMillis();
    rangeBasedObjectIDSet.addAll(rangeBasedObjectIDSet2);   
    addAllTime = System.currentTimeMillis() - startTime;
    
    System.out.println("rangeSet.addAll random total time took: " + addAllTime + " ms. ");
    
    assertEquals(rangeSize + rangeSize2, rangeBasedObjectIDSet.size());
    
  }
  
  
  private void addToReferencesSerial(ObjectIDSet set, int size) {
    for (int i = 2 * size; i < size + (2 * size); i++) {
      set.add(new ObjectID(i));
    }
  }

  private void addToReferencesRandom(final ObjectIDSet set, final int size) {
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testContain : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < size; i++) {
      set.add(new ObjectID(r.nextInt(size)));
    }
  }

  private void removeElementFromIterator(final Iterator i, final int totalElements, final long indexOfRemoveElement,
                                         final int nextExpectedElement) {
    long visitedElements = 0;
    visitedElements += iterateElements(i, indexOfRemoveElement);
    i.remove();
    assertEquals(nextExpectedElement, ((ObjectID) i.next()).toLong());
    visitedElements += iterateElements(i);
    assertEquals(visitedElements, totalElements - 1);
  }
}

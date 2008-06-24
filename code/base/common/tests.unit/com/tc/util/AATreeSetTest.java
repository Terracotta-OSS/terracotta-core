/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public class AATreeSetTest extends TCTestCase {

  public void testRandom() {
    List longs = populateRandomLongs(new ArrayList(), 1000);

    TreeSet treeSet = new TreeSet();
    AATreeSet aatree = new AATreeSet();

    for (Iterator i = longs.iterator(); i.hasNext();) {
      Long l = (Long) i.next();
      boolean aaInsert = aatree.insert(l);
      boolean tsInsert = treeSet.add(l);
      assertEquals(tsInsert, aaInsert);
    }

    assertEquals(treeSet.size(), aatree.size());

    Iterator tsIterator = treeSet.iterator();
    Iterator aaIterator = aatree.iterator();

    while (tsIterator.hasNext() && aaIterator.hasNext()) {
      assertEquals(tsIterator.next(), aaIterator.next());
    }

    assertFalse(tsIterator.hasNext());
    assertFalse(aaIterator.hasNext());

  }

  public void testBasic() {

    AATreeSet t = new AATreeSet();
    t.insert(new Long(25));
    t.insert(new Long(10));
    t.insert(new Long(1));
    t.insert(new Long(4));
    t.insert(new Long(15));
    t.insert(new Long(8));
    t.insert(new Long(11));
    t.insert(new Long(10));
    t.insert(new Long(9));
    t.insert(new Long(13));
    t.insert(new Long(2));
    t.insert(new Long(23));
    t.insert(new Long(35));
    t.insert(new Long(33));
    t.insert(new Long(17));
    t.insert(new Long(29));
    t.insert(new Long(19));

    checkAATreeIteratorElements(t.iterator());
  }

  private void checkAATreeIteratorElements(Iterator i) {
    Comparable prev = new Long(Integer.MIN_VALUE);
    while (i.hasNext()) {
      Comparable curr = ((Comparable) i.next());
      // assert that iteration gives sorted result, since aatree behaves like a set, duplicates are eliminated and hence
      // we don't have to check for equality.
      Assert.eval(curr.compareTo(prev) > 0);
      prev = curr;
    }

  }

  public void testVeryBasic() {
    AATreeSet aaTree = new AATreeSet();
    boolean inserted = aaTree.insert(new Integer(10));
    assertTrue(inserted);
    assertEquals(1, aaTree.size());

    inserted = aaTree.insert(new Integer(10));
    assertFalse(inserted);
    assertEquals(1, aaTree.size());

    Comparable deleted = aaTree.remove(new Integer(100));
    assertNull(deleted);
    assertEquals(1, aaTree.size());

    deleted = aaTree.remove(new Integer(10));
    assertNotNull(deleted);
    assertEquals(0, aaTree.size());

    deleted = aaTree.remove(new Integer(10));
    assertNull(deleted);
    assertEquals(0, aaTree.size());

    inserted = aaTree.insert(new Integer(10));
    assertTrue(inserted);
    assertEquals(1, aaTree.size());

    aaTree.clear();
    assertEquals(0, aaTree.size());

  }

  // Test program; should print min and max and nothing else
  public void testMinMax() {
    AATreeSet t = new AATreeSet();
    final int NUMS = 400000;
    final int GAP = 307;

    t.insert(new Integer(NUMS * 2));
    t.insert(new Integer(NUMS * 3));
    int size = 2;
    for (int i = GAP; i != 0; i = (i + GAP) % NUMS) {
      t.insert(new Integer(i));
      size++;
    }
    System.out.println("Inserts complete");
    assertEquals(size, t.size());

    t.remove(t.findMax());
    for (int i = 1; i < NUMS; i += 2) {
      t.remove(new Integer(i));
    }
    t.remove(t.findMax());
    System.out.println("Removes complete");

    if (((Integer) (t.findMin())).intValue() != 2 || ((Integer) (t.findMax())).intValue() != NUMS - 2) { throw new AssertionError(
                                                                                                                                  "FindMin or FindMax error!"); }

    for (int i = 2; i < NUMS; i += 2) {
      if (((Integer) t.find(new Integer(i))).intValue() != i) { throw new AssertionError("Error: find fails for " + i); }
    }

    for (int i = 1; i < NUMS; i += 2) {
      if (t.find(new Integer(i)) != null) { throw new AssertionError("Error: Found deleted item " + i); }
    }
  }

  /* Not really testing anything, what ever */
  public void testDump() {
    AATreeSet t = new AATreeSet();
    System.out.println("Inserted = " + t.insert(new Integer(8)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(4)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(10)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(2)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(6)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(9)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(11)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(1)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(3)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(5)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(7)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(12)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(1)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(3)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));

    System.out.println("Deleted = " + t.remove(new Integer(6)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(8)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(10)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(12)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(6)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(8)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(1)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
  }

  public void testTailSetIteratorForRandomAATree() {
    System.err.println("TreeSet Creation");
    List longs = populateRandomLongs(new ArrayList(), 10001);
    TreeSet treeSet = new TreeSet();
    AATreeSet aatree = new AATreeSet();

    for (Iterator i = longs.iterator(); i.hasNext();) {
      Long l = (Long) i.next();
      boolean aaInsert = aatree.insert(l);
      boolean tsInsert = treeSet.add(l);
      assertEquals(tsInsert, aaInsert);
    }

    System.out.println("XXX AAtree : " + aatree.dump());
    assertEquals(treeSet.size(), aatree.size());

    System.err.println("TailSets from TreeSet");
    for (int i = 0; i < 501; i++) {
      SecureRandom sr = new SecureRandom();
      long seed = sr.nextLong();
      Random r = new Random(seed);
      Long tailSetKey = new Long(r.nextLong());
      System.err.println("Seed for random : " + seed + ". tailSetKey = " + tailSetKey);
      Iterator tsIterator = (treeSet.tailSet(tailSetKey)).iterator();
      Iterator aaIterator = aatree.tailSetIterator(tailSetKey);
      compareIterators(tsIterator, aaIterator);
    }
  }

  public void testTailSetIteratorsForFixedAATree() {

    // 1 2 4 8 9 10 11 13 15 17 19 23 25 27 29 33 35 47 56
    List longList = new ArrayList();
    longList.add(new Long(25));
    longList.add(new Long(27));
    longList.add(new Long(10));
    longList.add(new Long(1));
    longList.add(new Long(4));
    longList.add(new Long(15));
    longList.add(new Long(8));
    longList.add(new Long(56));
    longList.add(new Long(11));
    longList.add(new Long(10));
    longList.add(new Long(9));
    longList.add(new Long(13));
    longList.add(new Long(2));
    longList.add(new Long(23));
    longList.add(new Long(47));
    longList.add(new Long(35));
    longList.add(new Long(33));
    longList.add(new Long(17));
    longList.add(new Long(29));
    longList.add(new Long(19));

    AATreeSet aaTreeSet = new AATreeSet();
    TreeSet treeSet = new TreeSet();
    for (int i = 0; i < longList.size(); i++) {
      Long insertItem = (Long) longList.get(i);
      aaTreeSet.insert(insertItem);
      treeSet.add(insertItem);
    }

    System.out.println("XXX " + aaTreeSet.dump());

    // Exact match
    checkTailSets(treeSet, aaTreeSet, new Long(15));

    // non-existing member with-in the range
    checkTailSets(treeSet, aaTreeSet, new Long(16));

    // non-existing member below the range
    checkTailSets(treeSet, aaTreeSet, new Long(-10101));

    // non-existing member above the range
    checkTailSets(treeSet, aaTreeSet, new Long(50505));

    // the least member
    checkTailSets(treeSet, aaTreeSet, new Long(1));

    // the max member
    checkTailSets(treeSet, aaTreeSet, new Long(56));

    // these members are important to test. For more info, draw the aaTree for above
    // data, and you will know the reason
    checkTailSets(treeSet, aaTreeSet, new Long(4));
    checkTailSets(treeSet, aaTreeSet, new Long(10));
    checkTailSets(treeSet, aaTreeSet, new Long(13));
    checkTailSets(treeSet, aaTreeSet, new Long(17));
    checkTailSets(treeSet, aaTreeSet, new Long(33));
    checkTailSets(treeSet, aaTreeSet, new Long(25));
    checkTailSets(treeSet, aaTreeSet, new Long(47));

    // node with no left child
    checkTailSets(treeSet, aaTreeSet, new Long(27));
    // node with no right child
    checkTailSets(treeSet, aaTreeSet, new Long(11));
  }

  private void checkTailSets(TreeSet treeSet, AATreeSet aaTreeSet, Long tailKey) {
    System.out.println("XXX TailSetKey : " + tailKey);
    System.out.println("XXX __ TreeSet : " + dumpUsingIterator((treeSet.tailSet(tailKey)).iterator()));
    System.out.println("XXX AA TreeSet : " + dumpUsingIterator(aaTreeSet.tailSetIterator(tailKey)));
    Iterator tsIterator = (treeSet.tailSet(tailKey)).iterator();
    Iterator aaIterator = aaTreeSet.tailSetIterator(tailKey);
    compareIterators(tsIterator, aaIterator);
  }

  private void compareIterators(Iterator tsIterator, Iterator aaIterator) {
    while (tsIterator.hasNext() && aaIterator.hasNext()) {
      assertEquals(tsIterator.next(), aaIterator.next());
    }
    assertFalse(tsIterator.hasNext());
    assertFalse(aaIterator.hasNext());
  }

  private static String dumpUsingIterator(AATreeSet t) {
    StringBuffer sb = new StringBuffer();
    for (Iterator i = t.iterator(); i.hasNext();) {
      sb.append(i.next());
      if (i.hasNext()) {
        sb.append(',');
      }
    }
    return sb.toString();
  }

  private static String dumpUsingIterator(Iterator i) {
    StringBuffer sb = new StringBuffer();
    while (i.hasNext()) {
      sb.append(i.next());
      if (i.hasNext()) {
        sb.append(',');
      }
    }
    return sb.toString();
  }

  private List populateRandomLongs(ArrayList arrayList, int count) {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    System.err.println("Seed for random : " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < count; i++) {
      arrayList.add(new Long(r.nextLong()));
    }
    return arrayList;
  }

}

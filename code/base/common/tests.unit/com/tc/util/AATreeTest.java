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

public class AATreeTest extends TCTestCase {

  public void testRandom() {
    List longs = populateRandomLongs(new ArrayList(), 1000);

    TreeSet treeSet = new TreeSet();
    AATree aatree = new AATree();

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

    AATree t = new AATree();
    t.insert(new Integer(25));
    t.insert(new Integer(10));
    t.insert(new Integer(1));
    t.insert(new Integer(4));
    t.insert(new Integer(15));
    t.insert(new Integer(8));
    t.insert(new Integer(11));
    t.insert(new Integer(10));
    t.insert(new Integer(9));
    t.insert(new Integer(13));
    t.insert(new Integer(2));
    t.insert(new Integer(23));
    t.insert(new Integer(35));
    t.insert(new Integer(33));
    t.insert(new Integer(17));
    t.insert(new Integer(29));
    t.insert(new Integer(19));

    Iterator i = t.iterator();
    Comparable prev = new Integer(Integer.MIN_VALUE);
    while (i.hasNext()) {
      Comparable curr = ((Comparable) i.next());
      // assert that iteration gives sorted result, since aatree behaves like a set, duplicates are eliminated and hence
      // we don't have to check for equality.
      Assert.eval(curr.compareTo(prev) > 0);
      prev = curr;
    }
  }

  public void testVeryBasic() {
    AATree aaTree = new AATree();
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
    AATree t = new AATree();
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

    if (((Integer) (t.findMin())).intValue() != 2 || ((Integer) (t.findMax())).intValue() != NUMS - 2) {
      throw new AssertionError("FindMin or FindMax error!");
    }

    for (int i = 2; i < NUMS; i += 2) {
      if (((Integer) t.find(new Integer(i))).intValue() != i)  {
        throw new AssertionError("Error: find fails for " + i);
      }
    }

    for (int i = 1; i < NUMS; i += 2) {
      if (t.find(new Integer(i)) != null) {
        throw new AssertionError("Error: Found deleted item " + i);
      }
    }
  }
  
  /* Not really testing anything, what ever */
  public void testDump() {
    AATree t = new AATree();
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
  
  private static String dumpUsingIterator(AATree t) {
    StringBuffer sb = new StringBuffer();
    for (Iterator i = t.iterator(); i.hasNext();) {
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

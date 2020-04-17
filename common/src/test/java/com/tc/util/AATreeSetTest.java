/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import com.tc.text.Banner;
import com.tc.util.AATreeSet.Node;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

public class AATreeSetTest extends TestCase {

  public void testRandom() {
    List<Long> longs = populateRandomLongs(new ArrayList<Long>(), 1000);

    SortedSet<Long> treeSet = new TreeSet<Long>();
    SortedSet<Long> aatree = new AATreeSet<Long>();

    for (Long l : longs) {
      boolean aaInsert = aatree.add(l);
      boolean tsInsert = treeSet.add(l);
      assertEquals(tsInsert, aaInsert);
    }

    assertEquals(treeSet.size(), aatree.size());

    Iterator<Long> tsIterator = treeSet.iterator();
    Iterator<Long> aaIterator = aatree.iterator();

    while (tsIterator.hasNext() && aaIterator.hasNext()) {
      assertEquals(tsIterator.next(), aaIterator.next());
    }

    assertFalse(tsIterator.hasNext());
    assertFalse(aaIterator.hasNext());

  }

  public void testBasic() {
    Set<Long> t = new AATreeSet<Long>();
    t.add(Long.valueOf(25));
    t.add(Long.valueOf(10));
    t.add(Long.valueOf(1));
    t.add(Long.valueOf(4));
    t.add(Long.valueOf(15));
    t.add(Long.valueOf(8));
    t.add(Long.valueOf(11));
    t.add(Long.valueOf(10));
    t.add(Long.valueOf(9));
    t.add(Long.valueOf(13));
    t.add(Long.valueOf(2));
    t.add(Long.valueOf(23));
    t.add(Long.valueOf(35));
    t.add(Long.valueOf(33));
    t.add(Long.valueOf(17));
    t.add(Long.valueOf(29));
    t.add(Long.valueOf(19));

    checkAATreeIteratorElements(t.iterator());
  }

  private static void checkAATreeIteratorElements(Iterator<Long> i) {
    Long prev = null;
    while (i.hasNext()) {
      Long curr = i.next();
      // assert that iteration gives sorted result, since aatree behaves like a set, duplicates are eliminated and hence
      // we don't have to check for equality.
      if (prev != null) {
        assertTrue("previous:" + prev + " current:" + curr, curr.compareTo(prev) > 0);
      }
      prev = curr;
    }

  }

  public void testVeryBasic() {
    AATreeSet<Integer> aaTree = new AATreeSet<Integer>();
    boolean inserted = aaTree.add(Integer.valueOf(10));
    assertTrue(inserted);
    assertEquals(1, aaTree.size());

    inserted = aaTree.add(Integer.valueOf(10));
    assertFalse(inserted);
    assertEquals(1, aaTree.size());

    assertFalse(aaTree.remove(Integer.valueOf(100)));
    assertEquals(1, aaTree.size());

    Integer deleted = aaTree.removeAndReturn(Integer.valueOf(10));
    assertNotNull(deleted);
    assertEquals(10, deleted.intValue());
    assertEquals(0, aaTree.size());

    deleted = aaTree.removeAndReturn(Integer.valueOf(10));
    assertNull(deleted);
    assertEquals(0, aaTree.size());

    inserted = aaTree.add(Integer.valueOf(10));
    assertTrue(inserted);
    assertEquals(1, aaTree.size());

    aaTree.clear();
    assertEquals(0, aaTree.size());

  }

  // Test program; should print min and max and nothing else
  public void testMinMax() {
    AATreeSet<Integer> t = new AATreeSet<Integer>();
    final int NUMS = 400000;
    final int GAP = 1;

    t.add(Integer.valueOf(NUMS * 2));
    t.add(Integer.valueOf(NUMS * 3));
    int size = 2;
    for (int i = GAP; i != 0; i = (i + GAP) % NUMS) {
      t.add(Integer.valueOf(i));
      size++;
    }
    assertEquals(size, t.size());

    assertTrue(t.remove(t.last()));
    for (int i = 1; i < NUMS; i += 2) {
      assertTrue("remove(" + i + ")", t.remove(Integer.valueOf(i)));
    }
    assertTrue(t.remove(t.last()));

    assertEquals("first()", 2, t.first().intValue());
    assertEquals("last()", NUMS - 2, t.last().intValue());

    for (int i = 2; i < NUMS; i += 2) {
      assertEquals("find(" + i + ")", Integer.valueOf(i), t.find(Integer.valueOf(i)));
    }

    for (int i = 1; i < NUMS; i += 2) {
      assertEquals("find(" + i + ")", null, t.find(Integer.valueOf(i)));
    }
  }

  public void testTailSetIteratorForRandomAATree() {
    System.err.println("TreeSet Creation");
    List<Long> longs = populateRandomLongs(new ArrayList<Long>(), 10001);
    SortedSet<Long> treeSet = new TreeSet<Long>();
    SortedSet<Long> aatree = new AATreeSet<Long>();

    for (Long l : longs) {
      boolean aaInsert = aatree.add(l);
      boolean tsInsert = treeSet.add(l);
      assertEquals(tsInsert, aaInsert);
    }

    assertEquals(treeSet.size(), aatree.size());

    for (int i = 0; i < 501; i++) {
      SecureRandom sr = new SecureRandom();
      long seed = sr.nextLong();
      Random r = new Random(seed);
      Long tailSetKey = Long.valueOf(r.nextLong());
      System.err.println("Seed for random : " + seed + ". tailSetKey = " + tailSetKey);
      Iterator<Long> tsIterator = treeSet.tailSet(tailSetKey).iterator();
      Iterator<Long> aaIterator = aatree.tailSet(tailSetKey).iterator();
      compareIterators(tsIterator, aaIterator);
    }
  }

  public void testTailSetIteratorsForFixedAATree() {

    // 1 2 4 8 9 10 11 13 15 17 19 23 25 27 29 33 35 47 56
    List<Long> longList = new ArrayList<Long>();
    longList.add(Long.valueOf(25));
    longList.add(Long.valueOf(27));
    longList.add(Long.valueOf(9));
    longList.add(Long.valueOf(13));
    longList.add(Long.valueOf(2));
    longList.add(Long.valueOf(23));
    longList.add(Long.valueOf(47));
    longList.add(Long.valueOf(35));
    longList.add(Long.valueOf(10));
    longList.add(Long.valueOf(1));
    longList.add(Long.valueOf(4));
    longList.add(Long.valueOf(15));
    longList.add(Long.valueOf(8));
    longList.add(Long.valueOf(56));
    longList.add(Long.valueOf(11));
    longList.add(Long.valueOf(10));
    longList.add(Long.valueOf(33));
    longList.add(Long.valueOf(17));
    longList.add(Long.valueOf(29));
    longList.add(Long.valueOf(19));

    AATreeSet<Long> aaTreeSet = new AATreeSet<Long>();
    TreeSet<Long> treeSet = new TreeSet<Long>();
    for (int i = 0; i < longList.size(); i++) {
      Long insertItem = longList.get(i);
      aaTreeSet.add(insertItem);
      treeSet.add(insertItem);
    }

    // Exact match
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(15));

    // non-existing member with-in the range
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(16));

    // non-existing member below the range
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(-10101));

    // non-existing member above the range
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(50505));

    // the least member
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(1));

    // the max member
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(56));

    // these members are important to test. For more info, draw the aaTree for above
    // data, and you will know the reason
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(4));
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(10));
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(13));
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(17));
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(33));
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(25));
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(47));

    // node with no left child
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(27));
    // node with no right child
    checkTailSets(treeSet, aaTreeSet, Long.valueOf(11));
  }

  private void checkTailSets(TreeSet<Long> treeSet, AATreeSet<Long> aaTreeSet, Long tailKey) {
    Iterator<Long> tsIterator = treeSet.tailSet(tailKey).iterator();
    Iterator<Long> aaIterator = aaTreeSet.tailSet(tailKey).iterator();
    compareIterators(tsIterator, aaIterator);
  }

  private void compareIterators(Iterator<Long> tsIterator, Iterator<Long> aaIterator) {
    while (tsIterator.hasNext() && aaIterator.hasNext()) {
      assertEquals(tsIterator.next(), aaIterator.next());
    }
    assertFalse(tsIterator.hasNext());
    assertFalse(aaIterator.hasNext());
  }

  private List<Long> populateRandomLongs(List<Long> list, int count) {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    System.err.println("Seed for random : " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < count; i++) {
      list.add(Long.valueOf(r.nextLong()));
    }
    return list;
  }

  public void testRemove() {
    AATreeSet<MyInt> aaTree = new AATreeSet<MyInt>();
    assertTrue(aaTree.add(new MyInt(5)));
    assertTrue(aaTree.add(new MyInt(10)));
    assertTrue(aaTree.add(new MyInt(1)));
    assertTrue(aaTree.add(new MyInt(7)));
    assertTrue(aaTree.add(new MyInt(12)));
    assertTrue(aaTree.add(new MyInt(11)));

    MyInt ten = aaTree.removeAndReturn(new MyInt(10));
    assertEquals(new MyInt(10), ten);

    MyInt five = aaTree.removeAndReturn(new MyInt(5));
    assertEquals(new MyInt(5), five);

    MyInt none = aaTree.removeAndReturn(new MyInt(13));
    assertNull(none);

    MyInt eleven = aaTree.removeAndReturn(new MyInt(11));
    assertEquals(new MyInt(11), eleven);

    assertEquals(3, aaTree.size());
    Iterator<MyInt> i = aaTree.iterator();
    MyInt one = i.next();
    assertEquals(new MyInt(1), one);
    aaTree.remove(one);

    assertEquals(2, aaTree.size());
    i = aaTree.iterator();
    MyInt seven = i.next();
    assertEquals(new MyInt(7), seven);
    aaTree.remove(seven);

    assertEquals(1, aaTree.size());
    MyInt twelve = aaTree.removeAndReturn(new MyInt(12));
    assertEquals(new MyInt(12), twelve);

    assertTrue(aaTree.isEmpty());
  }

  public void testTreeBalance() {
    SortedSet<Long> treeSet = new TreeSet<Long>();
    AATreeSet<Long> aaTreeSet = new AATreeSet<Long>();

    long seed = System.nanoTime();
    Banner.infoBanner("Tree Balance Test Seed Is " + seed);
    Random rndm = new Random(seed);
    for (int i = 0; i < 10000; i++) {
      if (rndm.nextFloat() < 0.25 && !treeSet.isEmpty()) {
        Iterator<Long> it = treeSet.iterator();
        Long l = it.next();
        for (int position = rndm.nextInt(treeSet.size()); position > 0; --position) {
          l = it.next();
        }
        boolean aaRemove = aaTreeSet.remove(l);
        boolean tsRemove = treeSet.remove(l);
        assertEquals(tsRemove, aaRemove);
      } else {
        Long l = rndm.nextLong();
        boolean aaInsert = aaTreeSet.add(l);
        boolean tsInsert = treeSet.add(l);
        assertEquals(tsInsert, aaInsert);
      }
      assertEquals(treeSet.size(), aaTreeSet.size());
      aaTreeSet.validateTreeStructure();
    }

    Iterator<Long> tsIterator = treeSet.iterator();
    Iterator<Long> aaIterator = aaTreeSet.iterator();

    while (tsIterator.hasNext() && aaIterator.hasNext()) {
      assertEquals(tsIterator.next(), aaIterator.next());
    }

    assertFalse(tsIterator.hasNext());
    assertFalse(aaIterator.hasNext());
  }

  private class MyInt extends AATreeSet.AbstractTreeNode<MyInt> implements Comparable<MyInt> {

    private int i;

    public MyInt(int i) {
      this.i = i;
    }

    @Override
    public int compareTo(MyInt o) {
      return this.i - o.i;
    }

    @Override
    public int hashCode() {
      return this.i;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof MyInt) { return this.i == ((MyInt) o).i; }
      return false;
    }

    @Override
    public String toString() {
      return "MyInt[" + this.i + "]";
    }

    @Override
    public void swapPayload(Node<MyInt> node) {
      MyInt myInt = (MyInt) node;
      int temp = myInt.i;
      myInt.i = this.i;
      this.i = temp;
    }

    @Override
    public MyInt getPayload() {
      return this;
    }

  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class SleepycatPersistableSetTest extends TCTestCase {

  private SleepycatPersistableSet set = null;

  protected void setUp() throws Exception {
    super.setUp();
    set = new SleepycatPersistableSet(new ObjectID(12));
  }

  public void testBasic() {
    final int NUMBERS_ADDED = 250 * 4;

    addNumbers(set, 0, NUMBERS_ADDED);
    assertSize(set, NUMBERS_ADDED);

    addNumbers(set, 0, NUMBERS_ADDED);
    assertSize(set, NUMBERS_ADDED);

    clearSet(set);
    assertSize(set, 0);

    addAllFromCollection(set, 0, NUMBERS_ADDED);
    assertSize(set, NUMBERS_ADDED);

    assertContains(set, 0, NUMBERS_ADDED);
    assertContainsAllAndEquals(set, 0, NUMBERS_ADDED);

    checkIterator(set, 0, NUMBERS_ADDED);
    checkRemove(set, 3 * NUMBERS_ADDED / 4, NUMBERS_ADDED / 4, 3 * NUMBERS_ADDED / 4);
    checkRemoveAll(set, NUMBERS_ADDED / 2, NUMBERS_ADDED / 4, NUMBERS_ADDED / 2);
    assertSize(set, NUMBERS_ADDED / 2);

    checkRetainAll(set, 0, NUMBERS_ADDED / 4, NUMBERS_ADDED / 4);
    checkToArray(set, 0, NUMBERS_ADDED / 4);
  }

  private void clearSet(Set s) {
    s.clear();
    assertEmpty(s);
  }

  private void assertContains(Set s, int start, int length) {
    for (int i = start; i < start + length; i++)
      assertTrue(s.contains(new Node(i)));
  }

  private void assertContainsAllAndEquals(Set s,int start, int length) {
    HashSet tempSet = new HashSet();
    for (int i = start; i < start + length; i++)
      tempSet.add(new Node(i));

    assertContainsAll(s, tempSet);
    assertEquals(s, tempSet);
  }

  private void assertContainsAll(Set s, Collection collection) {
    assertTrue(s.containsAll(collection));
  }

  public void assertEquals(Set s, Collection collection) {
    assertTrue(s.equals(collection));
  }

  private void assertEmpty(Set s) {
    assertTrue(s.isEmpty());
  }

  public void checkIterator(Set s, int start, int length) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    Iterator iter = s.iterator();
    assertNotNull(iter);

    while (iter.hasNext()) {
      assertTrue(vector.contains(iter.next()));
    }
  }

  private void checkRemove(Set s, int start, int length, int expectedLeft) {
    for (int i = start; i < start + length; i++)
      s.remove(new Node(i));
    assertSize(s, expectedLeft);
  }

  private void checkRemoveAll(Set s, int start, int length, int expectedLeft) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    s.removeAll(vector);
    assertSize(s, expectedLeft);
  }

  private void checkRetainAll(Set s, int start, int length, int expectedLeft) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    s.retainAll(vector);
    assertSize(s, expectedLeft);
  }

  private void assertSize(Set s, int expected) {
    assertEquals(expected, s.size());
  }

  private void checkToArray(Set s, int start, int length) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    Object[] objArray = s.toArray();
    assertEquals(length, objArray.length);

    for (int i = 0; i < length; i++)
      assertTrue(vector.contains(objArray[i]));
  }

  private void addNumbers(Set s, int start, int numbersToBeAdded) {
    for (int i = start; i < start + numbersToBeAdded; i++) {
      s.add(new Node(i));
    }
  }

  private void addAllFromCollection(Set s, int start, int numbersToBeAdded) {
    ArrayList list = new ArrayList(numbersToBeAdded);
    for (int i = start; i < start + numbersToBeAdded; i++)
      list.add(new Node(i));

    s.addAll(list);
  }

  private class Node {
    private int i;

    public Node(int i) {
      this.i = i;
    }

    public int getNumber() {
      return i;
    }

    @Override
    public boolean equals(Object obj) {
      Node number = (Node) obj;
      return number.i == this.i;
    }

    @Override
    public int hashCode() {
      return i;
    }
  }
}

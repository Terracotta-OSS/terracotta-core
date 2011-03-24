/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class DBPersistableSetTest extends TCTestCase {

  private TCPersistableSet set = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.set = new TCPersistableSet(new ObjectID(12), new HashMap());
  }

  public void testBasic() {
    final int NUMBERS_ADDED = 250 * 4;

    addNumbers(this.set, 0, NUMBERS_ADDED);
    assertSize(this.set, NUMBERS_ADDED);

    addNumbers(this.set, 0, NUMBERS_ADDED);
    assertSize(this.set, NUMBERS_ADDED);

    clearSet(this.set);
    assertSize(this.set, 0);

    addAllFromCollection(this.set, 0, NUMBERS_ADDED);
    assertSize(this.set, NUMBERS_ADDED);

    assertContains(this.set, 0, NUMBERS_ADDED);
    assertContainsAllAndEquals(this.set, 0, NUMBERS_ADDED);

    checkIterator(this.set, 0, NUMBERS_ADDED);
    checkRemove(this.set, 3 * NUMBERS_ADDED / 4, NUMBERS_ADDED / 4, 3 * NUMBERS_ADDED / 4);
    checkRemoveAll(this.set, NUMBERS_ADDED / 2, NUMBERS_ADDED / 4, NUMBERS_ADDED / 2);
    assertSize(this.set, NUMBERS_ADDED / 2);

    checkRetainAll(this.set, 0, NUMBERS_ADDED / 4, NUMBERS_ADDED / 4);
    checkToArray(this.set, 0, NUMBERS_ADDED / 4);
  }

  private void clearSet(final Set s) {
    s.clear();
    assertEmpty(s);
  }

  private void assertContains(final Set s, final int start, final int length) {
    for (int i = start; i < start + length; i++) {
      assertTrue(s.contains(new Node(i)));
    }
  }

  private void assertContainsAllAndEquals(final Set s, final int start, final int length) {
    final HashSet tempSet = new HashSet();
    for (int i = start; i < start + length; i++) {
      tempSet.add(new Node(i));
    }

    assertContainsAll(s, tempSet);
    assertEquals(s, tempSet);
  }

  private void assertContainsAll(final Set s, final Collection collection) {
    assertTrue(s.containsAll(collection));
  }

  public void assertEquals(final Set s, final Collection collection) {
    assertTrue(s.equals(collection));
  }

  private void assertEmpty(final Set s) {
    assertTrue(s.isEmpty());
  }

  public void checkIterator(final Set s, final int start, final int length) {
    final Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++) {
      vector.add(new Node(i));
    }

    final Iterator iter = s.iterator();
    assertNotNull(iter);

    while (iter.hasNext()) {
      assertTrue(vector.contains(iter.next()));
    }
  }

  private void checkRemove(final Set s, final int start, final int length, final int expectedLeft) {
    for (int i = start; i < start + length; i++) {
      s.remove(new Node(i));
    }
    assertSize(s, expectedLeft);
  }

  private void checkRemoveAll(final Set s, final int start, final int length, final int expectedLeft) {
    final Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++) {
      vector.add(new Node(i));
    }

    s.removeAll(vector);
    assertSize(s, expectedLeft);
  }

  private void checkRetainAll(final Set s, final int start, final int length, final int expectedLeft) {
    final Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++) {
      vector.add(new Node(i));
    }

    s.retainAll(vector);
    assertSize(s, expectedLeft);
  }

  private void assertSize(final Set s, final int expected) {
    assertEquals(expected, s.size());
  }

  private void checkToArray(final Set s, final int start, final int length) {
    final Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++) {
      vector.add(new Node(i));
    }

    final Object[] objArray = s.toArray();
    assertEquals(length, objArray.length);

    for (int i = 0; i < length; i++) {
      assertTrue(vector.contains(objArray[i]));
    }
  }

  private void addNumbers(final Set s, final int start, final int numbersToBeAdded) {
    for (int i = start; i < start + numbersToBeAdded; i++) {
      s.add(new Node(i));
    }
  }

  private void addAllFromCollection(final Set s, final int start, final int numbersToBeAdded) {
    final ArrayList list = new ArrayList(numbersToBeAdded);
    for (int i = start; i < start + numbersToBeAdded; i++) {
      list.add(new Node(i));
    }

    s.addAll(list);
  }

  private static class Node {
    private final int i;

    public Node(final int i) {
      this.i = i;
    }

    @Override
    public boolean equals(final Object obj) {
      final Node number = (Node) obj;
      return number.i == this.i;
    }

    @Override
    public int hashCode() {
      return this.i;
    }
  }
}

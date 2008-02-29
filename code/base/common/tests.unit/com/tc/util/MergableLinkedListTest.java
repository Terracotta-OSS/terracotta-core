/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class MergableLinkedListTest extends TestCase {

  public void testAddAll() {

    // initial list, assert correctness
    MergableLinkedList masterList = createMergableLinkedList(1,2);
    assertEquals(2, masterList.size());
    assertFalse(masterList.isEmpty());

    // collection to test add all, assert correctness
    Collection coll = createCollection(3,2);
    assertEquals(2, coll.size());
    assertFalse(coll.isEmpty());

    // add list, assert correctness
    masterList.addAll(coll);
    assertEquals(4, masterList.size());
    assertFalse(masterList.isEmpty());

    // test to see if collection was added to the
    // end of the list, as expected.
    // NOTE: mergableLinkedList does not have an iterator
    // one must remove first to check values
    TestData testData1 = (TestData) masterList.removeFirst();
    assertEquals("testData1", testData1.getData());
    assertEquals(3, masterList.size());
    TestData testData2 = (TestData) masterList.removeFirst();
    assertEquals("testData2", testData2.getData());
    assertEquals(2, masterList.size());
    TestData testData3 = (TestData) masterList.removeFirst();
    assertEquals("testData3", testData3.getData());
    assertEquals(1, masterList.size());
    TestData testData4 = (TestData) masterList.removeFirst();
    assertEquals("testData4", testData4.getData());
    assertEquals(0, masterList.size());

    // addAll does NOT clear original collection
    assertEquals(2, coll.size());

    // test null and empty case
    MergableLinkedList emptyList = new MergableLinkedList();
    try {
      emptyList.addAll(null);
      fail("adding a null exception should throw a null pointer exception.");
    } catch (NullPointerException npe) {
      assertEquals(0, emptyList.size());

    }

    emptyList.addAll(new ArrayList());
    assertEquals(0, emptyList.size());

  }

  public void testMergeToFront() {

    // initial list, assert correctness
    MergableLinkedList masterList = createMergableLinkedList(1,2);
    assertEquals(2, masterList.size());
    assertFalse(masterList.isEmpty());

    // initial list, assert correctness
    MergableLinkedList secondList = createMergableLinkedList(3,2);
    assertEquals(2, secondList.size());
    assertFalse(secondList.isEmpty());

    // mergeToFrom, assert correctness
    masterList.mergeToFront(secondList);
    assertEquals(4, masterList.size());
    assertFalse(masterList.isEmpty());

    // verify merged to the front
    TestData testData3 = (TestData) masterList.removeFirst();
    assertEquals("testData3", testData3.getData());
    assertEquals(3, masterList.size());
    TestData testData4 = (TestData) masterList.removeFirst();
    assertEquals("testData4", testData4.getData());
    assertEquals(2, masterList.size());
    TestData testData1 = (TestData) masterList.removeFirst();
    assertEquals("testData1", testData1.getData());
    assertEquals(1, masterList.size());
    TestData testData2 = (TestData) masterList.removeFirst();
    assertEquals("testData2", testData2.getData());
    assertEquals(0, masterList.size());

    // mergeToFront clears the second MergableLinkedList
    assertEquals(0, secondList.size());

    // test null and empty case
    MergableLinkedList emptyList = new MergableLinkedList();
    try {
      emptyList.mergeToFront(null);
      fail("adding null MergableLinkedList should throw a null pointer exception.");
    } catch (NullPointerException npe) {
      assertEquals(0, emptyList.size());
    }

    emptyList.mergeToFront(new MergableLinkedList());
    assertEquals(0, emptyList.size());

  }

  public void testRemoveFirst() {
    MergableLinkedList masterList = createMergableLinkedList(1,2);
    assertEquals(2, masterList.size());
    assertFalse(masterList.isEmpty());

    TestData testData1 = (TestData) masterList.removeFirst();
    assertEquals("testData1", testData1.getData());

    assertEquals(1, masterList.size());
    assertFalse(masterList.isEmpty());

    // empty case

    try {
      MergableLinkedList emptyList = new MergableLinkedList();
      emptyList.removeFirst();
      fail("should throw NoSuchElementException size list is empty");
    } catch (NoSuchElementException nsee) {
      // passed
    }
  }

  public void testIsEmpty() {
    MergableLinkedList emptyList = new MergableLinkedList();
    assertTrue(emptyList.isEmpty());

    MergableLinkedList populatedList = createMergableLinkedList(1,2);
    assertFalse(populatedList.isEmpty());
  }

  public void testAdd() {
    MergableLinkedList list = new MergableLinkedList();
    assertEquals(0, list.size());
    list.add(new TestData("testData1"));
    assertEquals(1, list.size());
    list.add(new TestData("testData2"));
    assertEquals(2, list.size());

    TestData testData1 = (TestData) list.removeFirst();
    assertEquals("testData1", testData1.getData());
    assertEquals(1, list.size());

    TestData testData2 = (TestData) list.removeFirst();
    assertEquals("testData2", testData2.getData());
    assertEquals(0, list.size());

    // test null and empty case

    MergableLinkedList emptyList = new MergableLinkedList();
    emptyList.add(null);

    assertEquals(1, emptyList.size());
    TestData data = (TestData) emptyList.removeFirst();
    assertNull(data);
  }

  public void testClear() {
    MergableLinkedList masterList = createMergableLinkedList(1,2);
    assertEquals(2, masterList.size());
    assertFalse(masterList.isEmpty());

    masterList.clear();
    assertEquals(0, masterList.size());
    assertTrue(masterList.isEmpty());

  }

  private MergableLinkedList createMergableLinkedList(int startValue, int size) {
    MergableLinkedList initialList = new MergableLinkedList();
    for (int i = startValue; i < startValue + size; i++) {
      initialList.add(new TestData("testData" + i));
    }
    return initialList;
  }

  private Collection createCollection(int startValue, int size) {
    Collection coll = new ArrayList();
    for(int i = startValue; i < startValue + size; i++) {
    coll.add(new TestData("testData" + i));
    }
    return coll;
  }

  private class TestData {
    private final String data;

    public TestData(String data) {
      this.data = data;
    }

    public String getData() {
      return data;
    }

  }

}

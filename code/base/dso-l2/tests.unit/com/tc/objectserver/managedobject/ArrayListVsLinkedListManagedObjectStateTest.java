/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class ArrayListVsLinkedListManagedObjectStateTest extends AbstractTestManagedObjectState {

  final int OBJ_COUNT = 50000;

  public void testCompareAdd() {
    TestDNACursor cursor = new TestDNACursor();
    add(cursor, OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, OBJ_COUNT);
    System.out.println("ArrayList.add for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime) + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, OBJ_COUNT);
    System.out
        .println("LinkedList.add for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime) + " ms.");
  }

  public void testCompareAddFirst() {
    TestDNACursor cursor = new TestDNACursor();
    addFirst(cursor, OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, OBJ_COUNT);
    System.out.println("ArrayList.addFirst for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, OBJ_COUNT);
    System.out.println("LinkedList.addFirst for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");
  }

  public void testCompareAddAt() {
    TestDNACursor cursor = new TestDNACursor();
    addAt(cursor, OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, OBJ_COUNT);
    System.out.println("ArrayList.addAt for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, OBJ_COUNT);
    System.out.println("LinkedList.addAt for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");
  }

  public void testCompareClear() {
    TestDNACursor cursor = new TestDNACursor();
    add(cursor, OBJ_COUNT);

    basicTestUnitArrayList(cursor, OBJ_COUNT);
 
    cursor.reset();

    bastTestUnitLinkedList(cursor, OBJ_COUNT);
  
    cursor.reset();

    clear(cursor);

    cursor.reset();

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, 0);
    System.out.println("ArrayList.clear for " + OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime)  + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, 0);
    System.out.println("LinkedList.clear for " + OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime) + " ms.");

  }

  public void testCompareRemoveFirst() {
    TestDNACursor cursor = new TestDNACursor();
    add(cursor, OBJ_COUNT);

    basicTestUnitArrayList(cursor, OBJ_COUNT);

    cursor.reset();

    bastTestUnitLinkedList(cursor, OBJ_COUNT);
 
    cursor.reset();

    removeFirst(cursor, OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, 0);
    System.out.println("ArrayList.removeFirst for " + OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime)  + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, 0);
    System.out.println("LinkedList.removeFirst for " + OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime) + " ms.");

  }

  public void testCompareRemove() {
    TestDNACursor cursor = new TestDNACursor();
    add(cursor, OBJ_COUNT);

    basicTestUnitArrayList(cursor, OBJ_COUNT);

    cursor.reset();

    bastTestUnitLinkedList(cursor, OBJ_COUNT);

    cursor.reset();

    remove(cursor, OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, 0);
    System.out.println("ArrayList.remove for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, 0);
    System.out.println("LinkedList.remove for " + OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

  }

  private void add(TestDNACursor cursor, int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(i) });
    }
  }

  private void addFirst(TestDNACursor cursor, int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(i) });
    }
  }

  private void addAt(TestDNACursor cursor, int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.ADD_AT, new Object[] { new Integer(i), new ObjectID(i) });
    }
  }

  private void clear(TestDNACursor cursor) {
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);
  }

  private void removeFirst(TestDNACursor cursor, int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.REMOVE_FIRST, null);
    }
  }

  private void remove(TestDNACursor cursor, int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { new ObjectID(i) });
    }
  }

  private void bastTestUnitLinkedList(TestDNACursor cursor, int objCount) {
    String className = "java.util.LinkedList";
    try {
      basicTestUnit(className, ManagedObjectState.LINKED_LIST_TYPE, cursor, objCount);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void basicTestUnitArrayList(TestDNACursor cursor, int objCount) {
    String className = "java.util.ArrayList";
    try {
      basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, objCount);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // override due to difference on dehydrate
  protected void basicDehydrate(TestDNACursor cursor, int objCount, ManagedObjectState state) {
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

}

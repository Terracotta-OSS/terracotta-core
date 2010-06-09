/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class ArrayListVsLinkedListManagedObjectStateTest extends AbstractTestManagedObjectState {

  final int OBJ_COUNT = 50000;

  public void testCompareAdd() {
    final TestDNACursor cursor = new TestDNACursor();
    add(cursor, this.OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, this.OBJ_COUNT);
    System.out.println("ArrayList.add for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, this.OBJ_COUNT);
    System.out.println("LinkedList.add for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");
  }

  public void testCompareAddFirst() {
    final TestDNACursor cursor = new TestDNACursor();
    addFirst(cursor, this.OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, this.OBJ_COUNT);
    System.out.println("ArrayList.addFirst for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, this.OBJ_COUNT);
    System.out.println("LinkedList.addFirst for " + this.OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime) + " ms.");
  }

  public void testCompareAddAt() {
    final TestDNACursor cursor = new TestDNACursor();
    addAt(cursor, this.OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, this.OBJ_COUNT);
    System.out.println("ArrayList.addAt for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, this.OBJ_COUNT);
    System.out.println("LinkedList.addAt for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");
  }

  public void testCompareClear() {
    final TestDNACursor cursor = new TestDNACursor();
    add(cursor, this.OBJ_COUNT);

    basicTestUnitArrayList(cursor, this.OBJ_COUNT);

    cursor.reset();

    bastTestUnitLinkedList(cursor, this.OBJ_COUNT);

    cursor.reset();

    clear(cursor);

    cursor.reset();

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, 0);
    System.out.println("ArrayList.clear for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, 0);
    System.out.println("LinkedList.clear for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

  }

  public void testCompareRemoveFirst() {
    final TestDNACursor cursor = new TestDNACursor();
    add(cursor, this.OBJ_COUNT);

    basicTestUnitArrayList(cursor, this.OBJ_COUNT);

    cursor.reset();

    bastTestUnitLinkedList(cursor, this.OBJ_COUNT);

    cursor.reset();

    removeFirst(cursor, this.OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, 0);
    System.out.println("ArrayList.removeFirst for " + this.OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime) + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, 0);
    System.out.println("LinkedList.removeFirst for " + this.OBJ_COUNT + " took "
                       + (System.currentTimeMillis() - startTime) + " ms.");

  }

  public void testCompareRemove() {
    final TestDNACursor cursor = new TestDNACursor();
    add(cursor, this.OBJ_COUNT);

    basicTestUnitArrayList(cursor, this.OBJ_COUNT);

    cursor.reset();

    bastTestUnitLinkedList(cursor, this.OBJ_COUNT);

    cursor.reset();

    remove(cursor, this.OBJ_COUNT);

    long startTime = System.currentTimeMillis();
    basicTestUnitArrayList(cursor, 0);
    System.out.println("ArrayList.remove for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

    cursor.reset();

    startTime = System.currentTimeMillis();
    bastTestUnitLinkedList(cursor, 0);
    System.out.println("LinkedList.remove for " + this.OBJ_COUNT + " took " + (System.currentTimeMillis() - startTime)
                       + " ms.");

  }

  private void add(final TestDNACursor cursor, final int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(i) });
    }
  }

  private void addFirst(final TestDNACursor cursor, final int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(i) });
    }
  }

  private void addAt(final TestDNACursor cursor, final int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.ADD_AT, new Object[] { new Integer(i), new ObjectID(i) });
    }
  }

  private void clear(final TestDNACursor cursor) {
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);
  }

  private void removeFirst(final TestDNACursor cursor, final int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.REMOVE_FIRST, null);
    }
  }

  private void remove(final TestDNACursor cursor, final int count) {
    for (int i = 1; i <= count; i++) {
      cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { new ObjectID(i) });
    }
  }

  private void bastTestUnitLinkedList(final TestDNACursor cursor, final int objCount) {
    final String className = "java.util.LinkedList";
    try {
      basicTestUnit(className, ManagedObjectState.LINKED_LIST_TYPE, cursor, objCount);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void basicTestUnitArrayList(final TestDNACursor cursor, final int objCount) {
    final String className = "java.util.ArrayList";
    try {
      basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, objCount);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

}

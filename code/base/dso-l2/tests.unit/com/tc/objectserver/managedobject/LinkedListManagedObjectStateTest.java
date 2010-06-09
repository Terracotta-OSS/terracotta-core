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

public class LinkedListManagedObjectStateTest extends AbstractTestManagedObjectState {

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

  /**
   * public void testArrayListVsLinkedList() { LinkedList linkedList = new LinkedList(); ArrayList arrayList = new
   * ArrayList(); for(int i = 0; i < 1000000; i++) { linkedList.add(new Integer(i)); arrayList.add(new Integer(i)); }
   * long startTime = System.currentTimeMillis(); for(int i = 0; i < 10000; i++) { arrayList.remove(i); }
   * System.out.println("removing 10000 elements out of a million from arrayList : " + (System.currentTimeMillis() -
   * startTime) + " ms."); startTime = System.currentTimeMillis(); for(int i = 0; i < 10000; i++) {
   * linkedList.remove(i); } System.out.println("removing 10000 elements out of a million from linkedList : " +
   * (System.currentTimeMillis() - startTime) + " ms."); }
   **/

  public void testObjectList1() throws Exception {
    final String className = "java.util.LinkedList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LINKED_LIST_TYPE, cursor, 3);
  }

  public void testObjectList2() throws Exception {
    final String className = "java.util.LinkedList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.ADD_AT, new Object[] { new Integer(1), new ObjectID(1000) });

    basicTestUnit(className, ManagedObjectState.LINKED_LIST_TYPE, cursor, 4);
  }

  public void testObjectList3() throws Exception {
    final String className = "java.util.LinkedList";
    final TestDNACursor cursor = new TestDNACursor();

    for (int i = 0; i < 1000; ++i) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(1000 + i) });
    }
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);

    basicTestUnit(className, ManagedObjectState.LINKED_LIST_TYPE, cursor, 0);
  }

  public void testObjectList4() throws Exception {
    final String className = "java.util.LinkedList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.REMOVE_FIRST, null);
    cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LINKED_LIST_TYPE, cursor, 1);
  }

}

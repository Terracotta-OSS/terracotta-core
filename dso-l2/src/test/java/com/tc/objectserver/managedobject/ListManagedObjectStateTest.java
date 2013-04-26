/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class ListManagedObjectStateTest extends AbstractTestManagedObjectState {

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

  public void testObjectList1() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 3);
  }

  public void testObjectList2() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.ADD_AT, new Object[] { Integer.valueOf(1), new ObjectID(1000) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 4);
  }

  public void testObjectList3() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    for (int i = 0; i < 1000; ++i) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(1000 + i) });
    }
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 0);
  }

  public void testObjectList4() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.REMOVE_FIRST, null);
    cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 1);
  }

  public void testInlineDGC() throws Exception {
    ObjectID oid = new ObjectID(0);
    ListManagedObjectState listManagedObjectState = new ListManagedObjectState(0);
    ApplyTransactionInfo applyTransactionInfo = Mockito.mock(ApplyTransactionInfo.class);
    for (int i = 2000; i < 2020; i++) {
      listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.ADD,
                                                new Object[] { new ObjectID(i) });
    }

    listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_FIRST, null);
    verify(applyTransactionInfo, times(1)).deleteObject(any(ObjectID.class));
    listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_AT,
                                              new Object[] { 0 });
    verify(applyTransactionInfo, times(2)).deleteObject(any(ObjectID.class));
    listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_RANGE, new Object[] {
        2, 4 });
    verify(applyTransactionInfo, times(4)).deleteObject(any(ObjectID.class));
    
    listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_LAST, null);
    verify(applyTransactionInfo, times(5)).deleteObject(any(ObjectID.class));
    
    listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.CLEAR, null);
    verify(applyTransactionInfo, times(20)).deleteObject(any(ObjectID.class));
    // try removing non-existent object
    listManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE,
                                              new Object[] { new ObjectID(2033) });
    verify(applyTransactionInfo, times(20)).deleteObject(any(ObjectID.class));
  }

}

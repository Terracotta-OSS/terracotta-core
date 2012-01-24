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

public class ListManagedObjectStateTest extends AbstractTestManagedObjectState {

  static {
    ManagedObjectStateFactory.enableLegacyTypes();
  }

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

  public void testObjectList1() throws Exception {
    final String className = "java.util.ArrayList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 3);
  }

  public void testObjectList2() throws Exception {
    final String className = "java.util.ArrayList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD_FIRST, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.ADD_AT, new Object[] { Integer.valueOf(1), new ObjectID(1000) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 4);
  }

  public void testObjectList3() throws Exception {
    final String className = "java.util.ArrayList";
    final TestDNACursor cursor = new TestDNACursor();

    for (int i = 0; i < 1000; ++i) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(1000 + i) });
    }
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 0);
  }

  public void testObjectList4() throws Exception {
    final String className = "java.util.ArrayList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.REMOVE_FIRST, null);
    cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { new ObjectID(2004) });

    basicTestUnit(className, ManagedObjectState.LIST_TYPE, cursor, 1);
  }

}

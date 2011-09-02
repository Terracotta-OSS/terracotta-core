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

public class SetManagedObjectStateTest extends AbstractTestManagedObjectState {

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }

  public void testObjectTreeSet1() throws Exception {
    final String className = "java.util.TreeSet";
    final String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.TREE_SET_TYPE, cursor, 3, false);
  }

  public void testObjectTreeSet2() throws Exception {
    final String className = "java.util.TreeSet";
    final TestDNACursor cursor = new TestDNACursor();

    for (int i = 0; i < 1000; ++i) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(1000 + i) });
    }
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);

    basicTestUnit(className, ManagedObjectState.TREE_SET_TYPE, cursor, 0, false);

  }

}

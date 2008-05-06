/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class SetManagedObjectStateTest extends AbstractTestManagedObjectState {
  
  // override due to difference on dehydrate
  protected void basicDehydrate(TestDNACursor cursor, int objCount, ManagedObjectState state) {
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    Assert.assertEquals(objCount, dnaWriter.getActionCount());
  }
  
  public void testObjectTreeSet1() throws Exception {
    String className = "java.util.TreeSet";
    String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.TREE_SET_TYPE, cursor, 3);
  }
  
  public void testObjectTreeSet2() throws Exception {
    String className = "java.util.TreeSet";
    TestDNACursor cursor = new TestDNACursor();

    for(int i = 0; i < 1000; ++i) {
      cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(1000+i) });
    }
    cursor.addLogicalAction(SerializationUtil.CLEAR, null);

    basicTestUnit(className, ManagedObjectState.TREE_SET_TYPE, cursor, 0);
  }

}

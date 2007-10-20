/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;

public class SetManagedObjectStateTest extends AbstractTestManagedObjectState {
  
  public void testObjectTreeSet() throws Exception {
    String className = "java.util.TreeSet";
    String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.TREE_SET_TYPE, cursor, 3);
  }
}

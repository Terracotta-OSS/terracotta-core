/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;

public class ArrayManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testIntegerArray() throws Exception {
    final String className = "[java.lang.Integer";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Integer[] { new Integer(2002), new Integer(2003) });

    basicTestUnit(className, ManagedObjectState.ARRAY_TYPE, cursor, 0);
  }

  public void testObjectArray() throws Exception {
    final String className = "[java.lang.Object";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Object[] { new ObjectID(2002), new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.ARRAY_TYPE, cursor, 2);
  }

}

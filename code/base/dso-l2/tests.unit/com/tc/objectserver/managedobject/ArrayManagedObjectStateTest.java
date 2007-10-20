/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;

public class ArrayManagedObjectStateTest extends AbstractTestManagedObjectState {

/*
  public void testIntegerArray() throws Exception {
    String className = "[java.lang.Integer";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Integer[] { new Integer(2002), new Integer(2003) });

    basicTestUnit(className, ManagedObjectState.ARRAY_TYPE, cursor, 0);
    // failed when readFrom back, the literalType changed from 0 (Integer) to 10 (Object)
  }
  */
  
  public void testObjectArray() throws Exception {
    String className = "[java.lang.Object";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Object[] { new ObjectID(2002), new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.ARRAY_TYPE, cursor, 2);
  }

}

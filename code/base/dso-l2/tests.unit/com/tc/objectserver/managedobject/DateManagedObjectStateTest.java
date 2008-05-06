/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;

public class DateManagedObjectStateTest extends AbstractTestManagedObjectState {
  
  public void testObjectDate() throws Exception {
    String className = "java.util.Date";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.SET_TIME, new Long[] { new Long(System.currentTimeMillis()) });
    cursor.addLogicalAction(SerializationUtil.SET_NANOS, new Integer[] { new Integer(0) });

    basicTestUnit(className, ManagedObjectState.DATE_TYPE, cursor, 0);
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;

public class DateManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testObjectDate() throws Exception {
    final String className = "java.util.Date";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.SET_TIME, new Long[] { Long.valueOf(System.currentTimeMillis()) });
    cursor.addLogicalAction(SerializationUtil.SET_NANOS, new Integer[] { Integer.valueOf(0) });

    basicTestUnit(className, ManagedObjectState.DATE_TYPE, cursor, 0);
  }

}

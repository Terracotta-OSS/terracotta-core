/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;

public class URLManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testObjectURL() throws Exception {
    final String className = "java.net.URL";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.URL_SET, new Object[] { "http", "terracotta.org", new Integer(8080),
        "auth", "user:pass", "/test", "par1=val1", "ref" });

    basicTestUnit(className, ManagedObjectState.URL_TYPE, cursor, 0);
  }

}

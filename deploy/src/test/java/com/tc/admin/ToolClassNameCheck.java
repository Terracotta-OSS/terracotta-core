/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.server.TCServerMain;
import com.tc.test.TCTestCase;
import com.tc.util.ToolClassNames;

/**
 * These tests verify that the tool class names recorded in ToolClassNames
 * are still valid against the actual class names. If someone changes the
 * class name of TCStop or AdminClient, we want these tests to go off,
 * indicating a needed change in ToolClassNames.
 */
public class ToolClassNameCheck extends TCTestCase {

  public void testTCStopClassName() {
    assertEquals(TCStop.class.getName(), ToolClassNames.TC_STOP_CLASS_NAME);
  }

  public void testTCServerClassName() {
    assertEquals(TCServerMain.class.getName(), ToolClassNames.TC_SERVER_CLASS_NAME);
  }
}

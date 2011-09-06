/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

import com.tc.test.TCTestCase;
import com.tc.util.ToolClassNames;

/** 
 * These tests verify that the tool class names recorded in ToolClassNames
 * are still valid against the actual class names.  If someone changes the
 * class name of TCStop or AdminClient, we want these tests to go off,
 * indicating a needed change in ToolClassNames.
 */
public class BootJarToolClassNameCheck extends TCTestCase {
  
  public void testTCStopClassName() {
    assertEquals(BootJarTool.class.getName(), ToolClassNames.BOOT_JAR_TOOL_CLASS_NAME);
  }

}

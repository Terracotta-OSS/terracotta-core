/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * This interface defines the class names of various Terracotta tools.
 * The tools themselves are not included in the -api packages but the 
 * class names are so that tc-maven-plugin can invoke the tools 
 * by launching java executions with the names.
 */
public interface ToolClassNames {

  public static final String ADMIN_CONSOLE_CLASS_NAME = "com.tc.admin.AdminClient";  
  public static final String BOOT_JAR_TOOL_CLASS_NAME = "com.tc.object.tools.BootJarTool";
  public static final String TC_SERVER_CLASS_NAME = "com.tc.server.TCServerMain";
  public static final String TC_STOP_CLASS_NAME = "com.tc.admin.TCStop";
  
}

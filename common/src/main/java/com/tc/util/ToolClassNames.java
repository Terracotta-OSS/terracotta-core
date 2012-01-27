/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

/**
 * This interface defines the class names of various Terracotta tools.
 * The tools themselves are not included in the -api packages but the
 * class names are so that tc-maven-plugin can invoke the tools
 * by launching java executions with the names.
 */
public interface ToolClassNames {

  public static final String TC_SERVER_CLASS_NAME = "com.tc.server.TCServerMain";
  public static final String TC_STOP_CLASS_NAME   = "com.tc.admin.TCStop";

}

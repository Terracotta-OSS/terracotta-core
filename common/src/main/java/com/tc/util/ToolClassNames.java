/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

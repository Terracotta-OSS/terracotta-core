/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.terracotta.tools.cli.TIMGetTool;

public class ListCommandTest extends AbstractCommandTestCase {
  
  public void setUp() throws Exception {
    setupConfig("2.6.2", "1.0.0");
    copyResourceToFile("single-module-index.xml", getIndexFile());    
  }
  
  public void testList() throws Exception {
    TIMGetTool tool = new TIMGetTool("list", getConfigProps());
    assertEquals(0, tool.getActionLog().getActionCount());
  }
    
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.terracotta.tools.cli.TIMGetTool;

import java.io.File;

public class InstallForCommandTest extends AbstractCommandTestCase {
  
  public void setUp() throws Exception {
    setupConfig("2.6.2", "1.0.0");
    copyResourceToFile("single-module-index.xml", getIndexFile());    
  }
  
  public void tearDown() throws Exception { 
    getTempDir().delete();
  }
  
  /**
   * <pre>
   * Scenario:
   * - the tc-config lists the module with no version
   * 
   * Expected behavior: 
   * - Latest module is downloaded
   * </pre>
   */
  public void test_configNoVersion() throws Exception {
    String configFile = "tc-config-noversion.xml";
    File tcConfig = new File(getTempDir(), configFile);      
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.0.0", getRepoDir());
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.0", getRepoDir());
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getRepoDir());
    
    copyResourceToFile(configFile, tcConfig);
    
    TIMGetTool tool = new TIMGetTool("install-for --no-verify " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(1, log.getActionCount());
    assertTrue(log.installed("org.terracotta.modules", "tim-annotations", "1.1.1"));
  }
  
}

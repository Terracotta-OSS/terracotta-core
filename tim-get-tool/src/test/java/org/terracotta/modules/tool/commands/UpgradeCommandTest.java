/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.terracotta.tools.cli.TIMGetTool;

import java.io.File;

public class UpgradeCommandTest extends AbstractCommandTestCase {
  
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
   * 1) the latest module is already installed in the modules directory
   * 2) the tc-config lists the module with no version
   * 
   * Expected behavior: 
   * 1) Latest module is *not* downloaded
   * 2) tc-config is *not* updated
   * </pre>
   */
  public void test_latestInstalled_configNoVersion() throws Exception {
    File tcConfig = new File(getTempDir(), "tc-config-noversion.xml");  
    
    copyResourceToFile("tc-config-noversion.xml", tcConfig);
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getModulesDir());
    
    TIMGetTool tool = new TIMGetTool("upgrade " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(0, log.getActionCount());
  }
  
  /**
   * <pre>
   * Scenario:
   * 1) an older version of the module is installed in the modules directory
   * 2) the tc-config lists the module with no version
   * 
   * Expected behavior:
   * 1) the latest version of the module is downloaded and installed
   * 2) tc-config will not be updated (as no version means latest)
   * </pre>
   */
  public void test_latestNotInstalled_configNoVersion() throws Exception {
    File tcConfig = new File(getTempDir(), "tc-config-noversion.xml");  
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getRepoDir());

    copyResourceToFile("tc-config-noversion.xml", tcConfig);
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.0", getModulesDir());
    
    TIMGetTool tool = new TIMGetTool("upgrade --no-verify " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(1, log.getInstalledCount());
    assertTrue(log.installed("org.terracotta.modules", "tim-annotations", "1.1.1"));
  }
 
  /**
   * <pre>
   * Scenario:
   * 1) no versions of the modules are installed in the modules directory
   * 2) the tc-config lists the module with no version
   * 
   * Expected behavior:
   * 1) the latest version of the module is downloaded and installed
   * 2) tc-config will not be updated (as no version means latest)
   * </pre>
   */
  public void test_noModules_configNoVersion() throws Exception {
    File tcConfig = new File(getTempDir(), "tc-config-noversion.xml");  
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getRepoDir());

    copyResourceToFile("tc-config-noversion.xml", tcConfig);
    
    TIMGetTool tool = new TIMGetTool("upgrade --no-verify " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(1, log.getInstalledCount());
    assertTrue(log.installed("org.terracotta.modules", "tim-annotations", "1.1.1"));
  }

  /**
   * <pre>
   * Scenario:
   * 1) no versions of the modules are installed in the modules directory
   * 2) the tc-config lists the module with a version that's older
   * 
   * Expected behavior:
   * 1) the latest version of the module is downloaded and installed
   * 2) tc-config will be updated
   * </pre>
   */
  public void test_noModules_configOlderNonExistentVersion() throws Exception {
    String tcConfigName = "tc-config-older-version.xml";  // has 1.1.0
    File tcConfig = new File(getTempDir(), tcConfigName);  
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getRepoDir());

    copyResourceToFile(tcConfigName, tcConfig);
    
    TIMGetTool tool = new TIMGetTool("upgrade --no-verify " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(2, log.getActionCount());
    assertTrue(log.installed("org.terracotta.modules", "tim-annotations", "1.1.1"));
    assertTrue(log.updatedModuleVersion("org.terracotta.modules", "tim-annotations", "1.1.1"));
  }
  
  /**
   * <pre>
   * Scenario:
   * 1) old version of the module is installed in the modules directory
   * 2) the tc-config lists the module with a version that's older
   * 
   * Expected behavior:
   * 1) the latest version of the module is downloaded and installed
   * 2) tc-config will be updated
   * </pre>
   */
  public void test_olderInstalled_configOlderVersion() throws Exception {
    String tcConfigName = "tc-config-older-version.xml";  // has 1.1.0
    File tcConfig = new File(getTempDir(), tcConfigName);  
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getRepoDir());

    copyResourceToFile(tcConfigName, tcConfig);
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.0", getModulesDir());
    
    TIMGetTool tool = new TIMGetTool("upgrade --no-verify " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(2, log.getActionCount());
    assertTrue(log.installed("org.terracotta.modules", "tim-annotations", "1.1.1"));
    assertTrue(log.updatedModuleVersion("org.terracotta.modules", "tim-annotations", "1.1.1"));
  }
  
  /**
   * <pre>
   * Scenario:
   * 1) latest version of the module is installed in the modules directory
   * 2) the tc-config lists the module with a version that's latest
   * 
   * Expected behavior:
   * 1) No changes
   * </pre>
   */
  public void test_latestInstalled_configLatestVersion() throws Exception {
    String tcConfigName = "tc-config-latest-version.xml";  // has 1.1.1
    File tcConfig = new File(getTempDir(), tcConfigName);  
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getRepoDir());

    copyResourceToFile(tcConfigName, tcConfig);
    createDummyModule("org.terracotta.modules", "tim-annotations", "1.1.1", getModulesDir());
    
    TIMGetTool tool = new TIMGetTool("upgrade --no-verify " + tcConfig.getAbsolutePath(), getConfigProps());
    ActionLog log = tool.getActionLog();
    assertEquals(0, log.getActionCount());
  }
  
}

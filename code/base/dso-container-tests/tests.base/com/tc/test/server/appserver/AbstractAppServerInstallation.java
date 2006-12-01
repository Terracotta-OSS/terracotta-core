/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import org.apache.commons.io.FileUtils;

import com.tc.util.Assert;

import java.io.File;
import java.net.URL;

/**
 * Manages the installed environment pertaining to an appserver. This class is supplied as a constructor argument to
 * {@link AbstractAppServer}.
 */
public abstract class AbstractAppServerInstallation implements AppServerStartupEnvironment {

  private final String  majorVersion;
  private final String  minorVersion;
  private final File    workingDirectory;
  private final File    serverInstall;
  private final File    dataDirectory;
  private final File    sandboxDirectory;
  private final boolean isRepoInstall;

  /**
   * Use existing installation (example: CATALINA_HOME)
   */
  public AbstractAppServerInstallation(File home, File workingDir, String majorVersion, String minorVersion)
      throws Exception {
    Assert.assertTrue(home.isDirectory());
    Assert.assertTrue(workingDir.isDirectory());
    if (workingDir.list().length > 0) { throw new Exception("Installation directory: " + workingDir.getAbsolutePath()
                                                            + " contains legacy files. It must be empty to install."); }
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.serverInstall = home;
    this.isRepoInstall = false;
    this.workingDirectory = workingDir;
    (this.dataDirectory = new File(workingDirectory + File.separator + AppServerConstants.DATA_DIR)).mkdir();
    this.sandboxDirectory = workingDirectory;
    // description file for the working directory with filename indicating the server type. Can add more desciptive
    // information if needed.
    new File(workingDir + File.separator + serverType() + "-" + majorVersion + "." + minorVersion).createNewFile();
  }

  /**
   * Create installation from Terracotta internal container repository (not publicly available)
   */
  public AbstractAppServerInstallation(URL host, File serverDir, File workingDir, String majorVersion,
                                       String minorVersion) throws Exception {
    Assert.assertTrue(serverDir.isDirectory());
    Assert.assertTrue(workingDir.isDirectory());
    if (workingDir.list().length > 0) { throw new Exception("Installation directory: " + workingDir.getAbsolutePath()
                                                            + " contains legacy files. It must be empty to install."); }
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.isRepoInstall = true;
    this.serverInstall = ConcreteReadOnlyAppServerInstallation.create(host, serverDir, serverType(), majorVersion,
                                                                      minorVersion);

    this.workingDirectory = workingDir;
    (this.dataDirectory = new File(workingDirectory + File.separator + AppServerConstants.DATA_DIR)).mkdir();
    this.sandboxDirectory = workingDirectory;
    // description file for the working directory with filename indicating the server type. Can add more desciptive
    // information if needed.
    new File(workingDir + File.separator + serverType() + "-" + majorVersion + "." + minorVersion).createNewFile();
  }

  public final File getDataDirectory() {
    return dataDirectory;
  }

  public abstract String serverType();

  public final String majorVersion() {
    return majorVersion;
  }

  public final String minorVersion() {
    return minorVersion;
  }

  public final void uninstall() throws Exception {
    FileUtils.deleteDirectory(workingDirectory.getParentFile());
  }

  public final File workingDirectory() {
    return workingDirectory;
  }

  public final File serverInstallDirectory() {
    return serverInstall;
  }

  public File getSandboxDirectory() {
    return sandboxDirectory;
  }

  public boolean isRepoInstall() {
    return isRepoInstall;
  }
}

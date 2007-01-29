/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.dsoserver;

import com.tc.config.Directories;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Delegates to {@link ExtraProcessServerControl}
 */
public final class StandardDsoServer implements DsoServer {

  private ExtraProcessServerControl dsoServer;
  private static final String       domain  = "127.0.0.1";
  private static final List         jvmArgs = new ArrayList();

  public ServerResult start(ServerParameters rawParams) throws Exception {
    DsoServerParameters params = (DsoServerParameters) rawParams;
    jvmArgs.add("-Xmx128m");
    jvmArgs.add("-verbose:gc");
    // jvmArgs.add("-Dtc.classloader.writeToDisk=true");

    dsoServer = new ExtraProcessServerControl(new ExtraProcessServerControl.DebugParams(), domain, params.dsoPort(),
                                              params.jmxPort(), params.configFile().toString(), params.workingDir(),
                                              Directories.getInstallationRoot(), false, jvmArgs, "");
    dsoServer.writeOutputTo(params.outputFile());
    dsoServer.start(2 * 60 * 1000);

    return new DsoServerResult(params.dsoPort(), this);
  }

  public void stop() throws Exception {
    if (dsoServer != null) {
      dsoServer.shutdown();
      dsoServer.waitFor();
    } else {
      System.err.println("**** DSO server was not started so ignoring request to stop the server. ****");
    }
  }

  public void addJvmArgs(final List args) {
    if (dsoServer != null && dsoServer.isRunning()) { throw new AssertionError(
                                                                               "Attempting to add jvm args after DSO server has been started."); }
    if (args != null && !args.isEmpty()) {
      jvmArgs.addAll(args);
    }
  }

  public boolean isRunning() {
    if (dsoServer == null || !dsoServer.isRunning()) { return false; }
    return true;
  }
}

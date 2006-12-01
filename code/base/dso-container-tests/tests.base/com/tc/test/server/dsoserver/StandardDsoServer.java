/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.dsoserver;

import com.tc.config.Directories;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // jvmArgs.add("-verbose:gc");
    // jvmArgs.add("-Dtc.classloader.writeToDisk=true");

    // XXX: remove me once done debugging
    if (Os.isSolaris()) {
      String javaVer = System.getProperty("java.version", "");
      Pattern p = Pattern.compile("^1\\.(\\d)\\.\\d_(\\d\\d)$");
      Matcher m = p.matcher(javaVer);
      if (m.matches()) {
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        if ((major == 4 && minor >= 12) || (major == 5 && minor >= 7)) {
          System.err.println("**** Adding heap dump option for L2 ****");
          jvmArgs.add("-XX:+HeapDumpOnOutOfMemoryError");
        }
      }
    }

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

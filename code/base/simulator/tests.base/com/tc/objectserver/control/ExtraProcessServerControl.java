/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import org.apache.commons.lang.ArrayUtils;

import com.tc.admin.TCStop;
import com.tc.config.Directories;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.process.LinkedJavaProcess;
import com.tc.process.StreamCopier;
import com.tc.server.TCServerMain;
import com.tc.test.TestConfigObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ExtraProcessServerControl extends ServerControlBase {
  private final String        name;
  private final boolean       mergeOutput;

  protected LinkedJavaProcess process;
  protected final String      configFileLoc;
  protected final List        jvmArgs;
  private final File          runningDirectory;
  private final String        serverName;
  private File                out;
  private FileOutputStream    fileOut;
  private StreamCopier        outCopier;
  private StreamCopier        errCopier;

  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput)
      throws FileNotFoundException {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, mergeOutput);
  }

  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   String servername, List additionalJvmArgs) throws FileNotFoundException {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, null, Directories.getInstallationRoot(),
         mergeOutput, servername, additionalJvmArgs);
  }

  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput) throws FileNotFoundException {
    // 2006-07-11 andrew -- We should get rid of the reference to Directories.getInstallationRoot() here.
    // Tests don't run in an environment where such a thing even exists. If the server needs an
    // "installation directory", the tests should be creating one themselves.
    this(debugParams, host, dsoPort, adminPort, configFileLoc, null, Directories.getInstallationRoot(), mergeOutput,
         null, new ArrayList());
  }

  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, File runningDirectory, File installationRoot,
                                   boolean mergeOutput, String serverName, List additionalJvmArgs) {
    super(host, dsoPort, adminPort);
    this.serverName = serverName;
    jvmArgs = new ArrayList();

    if (additionalJvmArgs != null && additionalJvmArgs.size() > 0) {
      jvmArgs.addAll(additionalJvmArgs);
    }

    this.configFileLoc = configFileLoc;
    this.mergeOutput = mergeOutput;
    this.name = "DSO process @ " + getHost() + ":" + getDsoPort();
    this.runningDirectory = runningDirectory;
    jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_PROPERTY_NAME + "=" + installationRoot);
    jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME + "=true");
    debugParams.addDebugParamsTo(jvmArgs);
  }

  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, File runningDirectory, File installationRoot,
                                   boolean mergeOutput, List jvmArgs, String undefString) {
    super(host, dsoPort, adminPort);
    serverName = null;

    this.jvmArgs = new ArrayList();
    for (Iterator i = jvmArgs.iterator(); i.hasNext();) {
      String next = (String) i.next();
      if (!next.equals(undefString)) {
        this.jvmArgs.add(next);
      }
    }

    this.configFileLoc = configFileLoc;
    this.mergeOutput = mergeOutput;
    this.name = "DSO process @ " + getHost() + ":" + getDsoPort();
    this.runningDirectory = runningDirectory;
    this.jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_PROPERTY_NAME + "=" + installationRoot);
    this.jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME + "=true");
    debugParams.addDebugParamsTo(jvmArgs);
  }

  public void mergeSTDOUT() {
    this.process.mergeSTDOUT();
  }

  public void mergeSTDERR() {
    this.process.mergeSTDERR();
  }

  protected String getMainClassName() {
    return TCServerMain.class.getName();
  }
  
  protected File getJavaHome() {
    try {
      return new File(TestConfigObject.getInstance().getL2StartupJavaHome());
    }
    catch (Exception e) {
      return null;
    }
  }

  protected String[] getMainClassArguments() {
    if (serverName != null && !serverName.equals("")) {
      return new String[] { StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc,
          StandardTVSConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName };
    } else {
      return new String[] { StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc };
    }
  }

  public void writeOutputTo(File outputFile) {
    if (mergeOutput) { throw new IllegalStateException(); }
    this.out = outputFile;
  }

  public void start(long timeout) throws Exception {
    System.err.println("Starting " + this.name + ": jvmArgs=" + jvmArgs + ", main=" + getMainClassName()
                       + ", main args=" + ArrayUtils.toString(getMainClassArguments()));
    process = createLinkedJavaProcess();
    process.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));
    process.start();
    if (mergeOutput) {
      mergeSTDOUT();
      mergeSTDERR();
    } else if (out != null) {
      fileOut = new FileOutputStream(out);
      outCopier = new StreamCopier(process.STDOUT(), fileOut);
      errCopier = new StreamCopier(process.STDERR(), fileOut);
      outCopier.start();
      errCopier.start();
    }
    waitUntilStarted(timeout);
    System.err.println(this.name + " started.");
  }

  protected LinkedJavaProcess createLinkedJavaProcess() {
    LinkedJavaProcess rv = new LinkedJavaProcess(getMainClassName(), getMainClassArguments());
    rv.setDirectory(this.runningDirectory);
    File processJavaHome = getJavaHome();
    if (processJavaHome != null) {
      rv.setJavaHome(processJavaHome);
    }
    return rv;
  }

  public void crash() throws Exception {
    System.out.println("Crashing server " + this.name + "...");
    if (process != null) {
      process.destroy();
      waitUntilShutdown();
    }
    System.out.println(this.name + " crashed.");
  }

  public void attemptShutdown() throws Exception {
    System.out.println("Shutting down server " + this.name + "...");
    TCStop stopper = new TCStop(getHost(), getAdminPort());
    stopper.stop();
  }

  public void shutdown() throws Exception {
    attemptShutdown();
    waitUntilShutdown();
    System.out.println(this.name + " stopped.");
  }

  private void waitUntilStarted(long timeout) throws Exception {
    long timeoutTime = System.currentTimeMillis() + timeout;
    while (true) {
      if (isRunning()) return;
      if (System.currentTimeMillis() > timeoutTime) {
        //
        throw new RuntimeException("Timeout occurred waiting for server to start: " + timeout + " ms.");
      }
      Thread.sleep(1000);
    }
  }

  public void waitUntilShutdown() throws Exception {
    while (isRunning()) {
      Thread.sleep(1000);
    }
  }

  public int waitFor() throws Exception {
    int rv = process.waitFor();

    if (outCopier != null) {
      try {
        outCopier.join();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (errCopier != null) {
      try {
        errCopier.join();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (fileOut != null) {
      try {
        fileOut.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return rv;
  }

  public static final class DebugParams {
    private final boolean debug;
    private final int     debugPort;

    public DebugParams() {
      this(false, 0);
    }

    public DebugParams(int debugPort) {
      this(true, debugPort);
    }

    private DebugParams(boolean debug, int debugPort) {
      if (debugPort < 0) throw new AssertionError("Debug port must be >= 0: " + debugPort);
      this.debugPort = debugPort;
      this.debug = debug;
    }

    private void addDebugParamsTo(Collection jvmArgs) {
      if (debug) {
        jvmArgs.add("-Xdebug");
        String address = debugPort > 0 ? "address=" + debugPort + "," : "";
        jvmArgs.add("-Xrunjdwp:transport=dt_socket," + address + "server=y,suspend=n");
      }
    }
  }

}

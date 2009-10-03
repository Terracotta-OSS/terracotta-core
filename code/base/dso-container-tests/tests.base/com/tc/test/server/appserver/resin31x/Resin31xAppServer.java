/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.resin31x;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Replace;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.test.TestConfigObject;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AbstractAppServer;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.AppServerResult;
import com.tc.test.server.util.AppServerUtil;
import com.tc.util.PortChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resin3x AppServer implementation
 */
public final class Resin31xAppServer extends AbstractAppServer {
  private static final String JAVA_CMD           = System.getProperty("java.home") + File.separator + "bin"
                                                   + File.separator + "java";

  private static final long   START_STOP_TIMEOUT = 240 * 1000;

  private String              configFile;
  private String              instanceName;
  private File                instanceDir;

  private int                 resin_port         = 0;
  private int                 watchdog_port      = 0;
  private int                 cluster_port       = 0;
  private Thread              runner             = null;

  public Resin31xAppServer(Resin31xAppServerInstallation installation) {
    super(installation);
  }

  public ServerResult start(ServerParameters parameters) throws Exception {
    AppServerParameters params = (AppServerParameters) parameters;
    return startResin(params);
  }

  public void stop() throws Exception {
    final String[] cmd = new String[] { JAVA_CMD, "-jar",
        this.serverInstallDirectory() + File.separator + "lib" + File.separator + "resin.jar", "stop", "-conf",
        configFile };

    System.err.println("Stopping instance " + instanceName + "...");
    Result result = Exec.execute(cmd, null, null, this.serverInstallDirectory());
    if (result.getExitCode() != 0) {
      System.err.println(result);
    }

    if (runner != null) {
      runner.join(START_STOP_TIMEOUT);
      if (runner.isAlive()) {
        System.err.println("Instance " + instanceName + " on port " + resin_port + " still alive.");
      } else {
        System.err.println("Resin instance " + instanceName + " stopped");
      }
    }

  }

  private ServerResult startResin(AppServerParameters params) throws Exception {
    prepareDeployment(params);

    List cmd = new ArrayList();
    cmd.add(0, JAVA_CMD);
    cmd.add("-cp");
    cmd.add(TestConfigObject.getInstance().extraClassPathForAppServer());
    cmd.add("-jar");
    cmd.add(this.serverInstallDirectory() + File.separator + "lib" + File.separator + "resin.jar");
    cmd.add("start");
    cmd.add("-conf");
    cmd.add(configFile);
    cmd.add("-resin-home");
    cmd.add(this.serverInstallDirectory().getAbsolutePath());
    cmd.add("-root-directory");
    cmd.add(this.instanceDir.getAbsolutePath());
    cmd.add("-verbose");
    final String[] cmdArray = (String[]) cmd.toArray(new String[] {});
    final String nodeLogFile = new File(instanceDir + ".log").getAbsolutePath();
    System.err.println("Starting resin with cmd: " + cmd);
    runner = new Thread("runner for " + instanceName) {
      @Override
      public void run() {
        try {
          Result result = Exec.execute(cmdArray, nodeLogFile, null, instanceDir);
          if (result.getExitCode() != 0) {
            System.err.println(result);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    runner.start();
    System.err.println("Starting resin " + instanceName + " on port " + resin_port + "...");
    AppServerUtil.waitForPort(resin_port, START_STOP_TIMEOUT);
    System.err.println("Started " + instanceName + " on port " + resin_port);
    return new AppServerResult(resin_port, this);
  }

  private void prepareDeployment(AppServerParameters params) throws Exception {
    instanceName = params.instanceName();
    instanceDir = new File(sandboxDirectory(), instanceName);
    ensureDirectory(instanceDir);
    ensureDirectory(getConfDirectory());

    File webapps_dir = getWebappsDirectory();
    ensureDirectory(webapps_dir);

    // move wars into the correct location
    Map wars = params.wars();
    if (wars != null && wars.size() > 0) {
      Set war_entries = wars.entrySet();
      Iterator war_entries_it = war_entries.iterator();
      while (war_entries_it.hasNext()) {
        Map.Entry war_entry = (Map.Entry) war_entries_it.next();
        File war_file = (File) war_entry.getValue();
        FileUtils.copyFileToDirectory(war_file, webapps_dir);
      }
    }

    // setup deployment config
    PortChooser portChooser = new PortChooser();
    resin_port = portChooser.chooseRandomPort();
    watchdog_port = portChooser.chooseRandomPort();
    cluster_port = portChooser.chooseRandomPort();

    setProperties(params, resin_port, instanceDir);
    createConfigFile(params.jvmArgs().replaceAll("'", "").split("\\s+"));
  }

  private static void ensureDirectory(File dir) throws Exception {
    if (!dir.exists() && dir.mkdirs() == false) { throw new Exception("Can't create directory ("
                                                                      + dir.getAbsolutePath()); }
  }

  private File getWebappsDirectory() {
    return new File(instanceDir, "webapps");
  }

  private File getConfDirectory() {
    return new File(instanceDir, "conf");
  }

  private void createConfigFile(String[] jvmargs) throws IOException {
    File confFile = new File(getConfDirectory(), "resin.conf");
    configFile = confFile.getAbsolutePath();
    copyResource("resin.conf", confFile);
    replaceToken("@resin.servlet.port@", String.valueOf(resin_port), confFile);
    replaceToken("@resin.watchdog.port@", String.valueOf(watchdog_port), confFile);
    replaceToken("@resin.cluster.port@", String.valueOf(cluster_port), confFile);
    StringBuilder resinExtraJvmArgs = new StringBuilder();
    for (String ja : jvmargs) {
      resinExtraJvmArgs.append("<jvm-arg>").append(ja).append("</jvm-arg>").append("\n");
    }
    replaceToken("@resin.extra.jvmargs@", resinExtraJvmArgs.toString(), confFile);
  }

  private void copyResource(String name, File dest) throws IOException {
    InputStream in = getClass().getResourceAsStream(name);
    FileOutputStream out = new FileOutputStream(dest);
    try {
      IOUtils.copy(in, out);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  private void replaceToken(String token, String value, File file) {
    Replace replaceTask = new Replace();
    replaceTask.setProject(new Project());
    replaceTask.setFile(file);
    replaceTask.setToken(token);
    replaceTask.setValue(value);
    replaceTask.execute();
  }
}

package com.tc.test.server.appserver.was6x;

import org.apache.commons.io.IOUtils;

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
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Was6xAppServer extends AbstractAppServer {
  private static final String TERRACOTTA_PY      = "terracotta.py";
  private static final String DEPLOY_APPS_PY     = "deployApps.py";
  private static final String ENABLE_DSO_PY      = "enable-dso.py";
  private static final String DSO_JVMARGS        = "__DSO_JVMARGS";
  private static final String PORTS_DEF          = "ports.def";
  private static final int    START_STOP_TIMEOUT = 4 * 60;                                                       // 4
  // mins

  private String[]            scripts            = new String[] { DEPLOY_APPS_PY, TERRACOTTA_PY, ENABLE_DSO_PY };

  private String              policy             = "grant codeBase \"file:FILENAME\" {" + IOUtils.LINE_SEPARATOR
                                                   + "  permission java.security.AllPermission;"
                                                   + IOUtils.LINE_SEPARATOR + "};" + IOUtils.LINE_SEPARATOR;
  private String              instanceName;
  private String              dsoJvmArgs;
  private int                 webspherePort;
  private File                sandbox;
  private File                instanceDir;
  private File                pyScriptsDir;
  private File                webappDir;
  private File                portDefFile;
  private File                serverInstallDir;

  private Thread              serverThread;

  public Was6xAppServer(Was6xAppServerInstallation installation) {
    super(installation);
  }

  public ServerResult start(ServerParameters parameters) throws Exception {
    init(parameters);
    createPortFile();
    copyPythonScripts();
    deleteProfileIfExists();
    createProfile();
    deployWarFile();
    addTerracottaToServerPolicy();
    enableDSO();
    serverThread = new Thread() {
      public void run() {
        try {
          startWebsphere();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    serverThread.setDaemon(true);
    serverThread.start();
    AppServerUtil.waitForPort(webspherePort, START_STOP_TIMEOUT * 1000);
    System.out.println("Websphere instance " + instanceName + " started on port " + webspherePort);
    return new AppServerResult(webspherePort, this);
  }

  public void stop() throws Exception {
    try {
      stopWebsphere();
      System.out.println("Websphere instance " + instanceName + " stopped.");
    } catch (Exception e) {
      // ignored
    } finally {
      try {
        deleteProfile();
      } catch (Exception e2) {
        // ignored
      }
    }
  }

  private void createPortFile() throws Exception {
    PortChooser portChooser = new PortChooser();
    webspherePort = portChooser.chooseRandomPort();

    List lines = IOUtils.readLines(getClass().getResourceAsStream(PORTS_DEF));
    lines.set(0, (String) lines.get(0) + webspherePort);

    for (int i = 1; i < lines.size(); i++) {
      String line = (String) lines.get(i);
      lines.set(i, line + portChooser.chooseRandomPort());
    }

    writeLines(lines, portDefFile, false);
  }

  private void copyPythonScripts() throws Exception {
    for (int i = 0; i < scripts.length; i++) {
      copyResourceTo(scripts[i], new File(pyScriptsDir, scripts[i]));
    }
  }

  private void enableDSO() throws Exception {
    File terracotta_py = new File(pyScriptsDir, TERRACOTTA_PY);
    FileInputStream fin = new FileInputStream(terracotta_py);
    List lines = IOUtils.readLines(fin);
    fin.close();

    // replace __DSO_JVMARGS
    for (int i = 0; i < lines.size(); i++) {
      String line = (String) lines.get(i);
      if (line.indexOf(DSO_JVMARGS) > 0) {
        line = line.replaceFirst(DSO_JVMARGS, dsoJvmArgs);
        lines.set(i, line);
        break;
      }
    }

    writeLines(lines, terracotta_py, false);
    String[] args = new String[] { "-lang", "jython", "-connType", "NONE", "-profileName", instanceName, "-f",
        new File(pyScriptsDir, ENABLE_DSO_PY).getAbsolutePath() };
    executeCommand(instanceDir, "wsadmin", args, pyScriptsDir, "Error in enabling DSO for " + instanceName);
  }

  private void deleteProfile() throws Exception {
    String[] args = new String[] { "-delete", "-profileName", instanceName };
    executeCommand(serverInstallDir, "manageprofiles", args, serverInstallDir, "Error in deleting profile for "
                                                                               + instanceName);
  }

  private void createProfile() throws Exception {
    String defaultTemplate = new File(serverInstallDir.getAbsolutePath(), "profileTemplates/default").getAbsolutePath();
    String[] args = new String[] { "-create", "-templatePath", defaultTemplate, "-profileName", instanceName,
        "-profilePath", instanceDir.getAbsolutePath(), "-portsFile", portDefFile.getAbsolutePath(),
        "-enableAdminSecurity", "false", "-isDeveloperServer" };
    long start = System.currentTimeMillis();
    System.out.println("Creating profile for instance " + instanceName + "...");
    executeCommand(serverInstallDir, "manageprofiles", args, serverInstallDir, "Error in creating profile for "
                                                                               + instanceName);
    System.out.println("Profile created in: " + ((System.currentTimeMillis() - start) / (1000.0 * 60)) + " minutes.");
  }

  private void deleteProfileIfExists() throws Exception {
    String[] args = new String[] { "-listProfiles" };
    String output = executeCommand(serverInstallDir, "manageprofiles", args, serverInstallDir, "");
    if (output.indexOf(instanceName) >= 0) {
      args = new String[] { "-delete", "-profileName", instanceName };
      executeCommand(serverInstallDir, "manageprofiles", args, serverInstallDir, "Trying to clean up existing profile");
    }
  }

  private void addTerracottaToServerPolicy() throws Exception {
    String classpath = System.getProperty("java.class.path");
    Set set = new HashSet();
    String[] entries = classpath.split(File.pathSeparator);
    for (int i = 0; i < entries.length; i++) {
      File filename = new File(entries[i]);
      if (filename.isDirectory()) {
        set.add(filename);
      } else {
        set.add(filename.getParentFile());
      }
    }

    List lines = new ArrayList(set.size() + 1);
    for (Iterator it = set.iterator(); it.hasNext();) {
      lines.add(getPolicyFor((File) it.next()));
    }
    lines.add(getPolicyFor(new File(TestConfigObject.getInstance().normalBootJar())));

    writeLines(lines, new File(instanceDir, "properties/server.policy"), true);
  }

  private String getPolicyFor(File filename) {
    String entry = filename.getAbsolutePath().replace('\\', '/');

    if (filename.isDirectory()) {
      return policy.replaceFirst("FILENAME", entry + "/-");
    } else {
      return policy.replaceFirst("FILENAME", entry);
    }
  }

  private void copyResourceTo(String filename, File dest) throws Exception {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(dest);
      IOUtils.copy(getClass().getResourceAsStream(filename), fos);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  private void deployWarFile() throws Exception {
    String[] args = new String[] { "-lang", "jython", "-connType", "NONE", "-profileName", instanceName, "-f",
        new File(pyScriptsDir, DEPLOY_APPS_PY).getAbsolutePath(), webappDir.getAbsolutePath().replace('\\', '/') };
    System.out.println("Deploying war file in: " + webappDir);
    executeCommand(instanceDir, "wsadmin", args, pyScriptsDir, "Error in deploying warfile for " + instanceName);
    System.out.println("Done deploying war file in: " + webappDir);
  }

  private void startWebsphere() throws Exception {
    String[] args = new String[] { "server1", "-profileName", instanceName, "-trace", "-timeout",
        String.valueOf(START_STOP_TIMEOUT) };
    executeCommand(instanceDir, "startServer", args, instanceDir, "Error in starting " + instanceName);
  }

  private void stopWebsphere() throws Exception {
    String[] args = new String[] { "server1", "-profileName", instanceName };
    executeCommand(instanceDir, "stopServer", args, instanceDir, "Error in stopping " + instanceName);
    if (serverThread != null) {
      serverThread.join(START_STOP_TIMEOUT);
    }
  }

  private void init(ServerParameters parameters) {
    AppServerParameters params = (AppServerParameters) parameters;
    this.sandbox = sandboxDirectory();
    this.instanceName = params.instanceName();
    this.instanceDir = new File(sandbox, instanceName);
    this.webappDir = new File(sandbox, "data");
    this.pyScriptsDir = new File(webappDir, instanceName);
    pyScriptsDir.mkdirs();
    this.portDefFile = new File(pyScriptsDir, PORTS_DEF);
    this.serverInstallDir = serverInstallDirectory();

    String[] jvm_args = params.jvmArgs().replaceAll("'", "").replace('\\', '/').split("\\s+");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < jvm_args.length; i++) {
      sb.append("\"" + jvm_args[i] + "\"");
      if (i < jvm_args.length - 1) {
        sb.append(", ");
      }
    }
    dsoJvmArgs = sb.toString();
  }

  private String getScriptPath(File root, String scriptName) {
    String fullScriptName = Os.isWindows() ? scriptName + ".bat" : scriptName + ".sh";
    return new File(root.getAbsolutePath(), "bin/" + fullScriptName).getAbsolutePath();
  }

  private String executeCommand(File rootDir, String scriptName, String[] args, File workingDir, String errorMessage)
      throws Exception {
    String script = getScriptPath(rootDir, scriptName);
    String[] cmd = new String[args.length + 1];
    cmd[0] = script;
    System.arraycopy(args, 0, cmd, 1, args.length);
    System.out.println("Execute cmd: " + Arrays.asList(cmd));
    Result result = Exec.execute(cmd, null, null, workingDir == null ? instanceDir : workingDir);
    String output = result.getStdout() + IOUtils.LINE_SEPARATOR + result.getStderr();
    System.out.println(output);
    if (result.getExitCode() != 0) {
      System.err.println(errorMessage);
    }
    return output;
  }

  private void writeLines(List lines, File filename, boolean append) throws Exception {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename, append);
      IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, fos);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }
}

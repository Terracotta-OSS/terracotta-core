package com.tc.test.server.appserver.jetty6x;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;

import com.tc.lcp.CargoLinkedChildProcess;
import com.tc.lcp.HeartBeatService;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Jetty6xAppServer extends AbstractAppServer {
  private static final String JAVA_CMD           = System.getProperty("java.home") + File.separator + "bin"
                                                   + File.separator + "java";
  private static final String STOP_KEY           = "secret";
  private static final String JETTY_MAIN_CLASS   = "org.mortbay.start.Main";
  private static final long   START_STOP_TIMEOUT = 240 * 1000;

  private static final String target             = "<SystemProperty name=\"jetty.home\" default=\".\"/>/webapps";

  private String              configFile;
  private String              instanceName;
  private File                instanceDir;
  private File                workDir;

  private int                 jetty_port         = 0;
  private int                 stop_port          = 0;
  private Thread              runner             = null;

  public Jetty6xAppServer(Jetty6xAppServerInstallation installation) {
    super(installation);
  }

  public ServerResult start(ServerParameters parameters) throws Exception {
    AppServerParameters params = (AppServerParameters) parameters;
    return startJetty(params);
  }

  public void stop() throws Exception {
    final String[] cmd = new String[] { JAVA_CMD, "-DSTOP.PORT=" + stop_port, "-DSTOP.KEY=" + STOP_KEY, "-jar",
        "start.jar", "--stop" };

    System.err.println("Stopping instance " + instanceName + "...");
    Result result = Exec.execute(cmd, null, null, this.serverInstallDirectory());
    if (result.getExitCode() != 0) {
      System.err.println(result);
    }

    if (runner != null) {
      runner.join(START_STOP_TIMEOUT);
      if (runner.isAlive()) {
        System.err.println("Instance " + instanceName + " on port " + jetty_port + " still alive.");
      } else {
        System.err.println("jetty instance " + instanceName + " stopped");
      }
    }

  }

  private AppServerResult startJetty(AppServerParameters params) throws Exception {
    prepareDeployment(params);

    String[] jvmargs = params.jvmArgs().replaceAll("'", "").split("\\s+");
    List cmd = new ArrayList(Arrays.asList(jvmargs));
    cmd.add(0, JAVA_CMD);
    cmd.add("-cp");
    cmd.add(this.serverInstallDirectory() + File.separator + "start.jar" + File.pathSeparator
            + TestConfigObject.getInstance().extraClassPathForAppServer());
    cmd.add("-Djetty.home=" + this.serverInstallDirectory());
    cmd.add("-Djetty.port=" + jetty_port);
    cmd.add("-DSTOP.PORT=" + stop_port);
    cmd.add("-DSTOP.KEY=" + STOP_KEY);
    cmd.add("-Djava.io.tmpdir=" + workDir.getAbsolutePath());
    cmd.add(CargoLinkedChildProcess.class.getName());
    cmd.add(JETTY_MAIN_CLASS);
    cmd.add(String.valueOf(HeartBeatService.listenPort()));
    cmd.add(instanceDir.getAbsolutePath());
    cmd.add(configFile);

    final String[] cmdArray = (String[]) cmd.toArray(new String[] {});
    final String nodeLogFile = new File(instanceDir + ".log").getAbsolutePath();

    runner = new Thread("runner for " + instanceName) {
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
    System.err.println("Starting jetty " + instanceName + " on port " + jetty_port + "...");
    AppServerUtil.waitForPort(jetty_port, START_STOP_TIMEOUT);
    System.err.println("Started " + instanceName + " on port " + jetty_port);
    return new AppServerResult(jetty_port, this);
  }

  private void prepareDeployment(AppServerParameters params) throws Exception {
    instanceName = params.instanceName();
    instanceDir = new File(sandboxDirectory(), instanceName);
    ensureDirectory(instanceDir);

    File wars_dir = getWarsDirectory();
    ensureDirectory(wars_dir);

    // move wars into the correct location
    Map wars = params.wars();
    if (wars != null && wars.size() > 0) {

      Set war_entries = wars.entrySet();
      Iterator war_entries_it = war_entries.iterator();
      while (war_entries_it.hasNext()) {
        Map.Entry war_entry = (Map.Entry) war_entries_it.next();
        File war_file = (File) war_entry.getValue();
        File dest = new File(wars_dir, war_file.getName());
        createServerSpecificWar(war_file, dest);
      }
    }

    // setup deployment config
    PortChooser portChooser = new PortChooser();
    jetty_port = portChooser.chooseRandomPort();
    stop_port = portChooser.chooseRandomPort();

    workDir = new File(sandboxDirectory(), "work");
    workDir.mkdirs();

    File logsDir = new File(instanceDir, "logs");
    ensureDirectory(logsDir);

    setProperties(params, jetty_port, instanceDir);
    createConfigFile();
  }

  private void createServerSpecificWar(File src, File dest) throws Exception {
    FileUtils.copyFile(src, dest);

    File tmpDir = new File(instanceDir, "tmp");
    File webInf = new File(tmpDir, "WEB-INF");
    ensureDirectory(webInf);
    writeJettyXml(webInf, instanceName);

    Zip zip = new Zip();
    zip.setUpdate(true);
    zip.setDestFile(dest);
    zip.setBasedir(tmpDir);
    zip.setProject(new Project());
    zip.execute();
  }

  static private void writeJettyXml(File dest, String name) throws IOException {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(new File(dest, "jetty-web.xml"));
      fos.write(jettyWebXmlText(name).getBytes());
    } finally {
      IOUtils.closeQuietly(fos);
    }

  }

  private static void ensureDirectory(File dir) throws Exception {
    if (!dir.exists() && dir.mkdirs() == false) { throw new Exception("Can't create directory ("
                                                                      + dir.getAbsolutePath()); }
  }

  protected File getWarsDirectory() {
    return new File(instanceDir, "war");
  }

  private void createConfigFile() throws Exception {
    String origialConfig = this.serverInstallDirectory().getAbsolutePath() + File.separator + "etc" + File.separator
                           + "jetty.xml";
    if (new File(origialConfig).exists() == false) { throw new Exception(origialConfig + " doesn't exist."); }

    StringBuffer buffer = new StringBuffer(1024);
    BufferedReader in = null;
    PrintWriter out = null;

    try {
      in = new BufferedReader(new FileReader(origialConfig));
      String line;
      while ((line = in.readLine()) != null) {
        buffer.append(line).append("\n");
      }

      int startIndex = buffer.indexOf(target);
      if (startIndex > 0) {
        int endIndex = startIndex + target.length();
        buffer.replace(startIndex, endIndex, getWarsDirectory().getAbsolutePath());
      } else {
        throw new RuntimeException("Can't find target: " + target);
      }

      configFile = new File(instanceDir, "jetty.xml").getAbsolutePath();
      out = new PrintWriter(new FileWriter(configFile));
      out.println(buffer.toString());

    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  private static String jettyWebXmlText(String workerName) {
    String s = "<?xml version=\"1.0\"  encoding=\"ISO-8859-1\"?>\n";
    s += "<Configure class=\"org.mortbay.jetty.webapp.WebAppContext\">\n";
    s += "  <Get name=\"sessionHandler\">\n";
    s += "    <Get name=\"sessionManager\">\n";
    s += "      <Call name=\"setIdManager\">\n";
    s += "        <Arg>\n";
    s += "          <New class=\"org.mortbay.jetty.servlet.HashSessionIdManager\">\n";
    s += "            <Set name=\"WorkerName\">" + workerName + "</Set>\n";
    s += "          </New>\n";
    s += "        </Arg>\n";
    s += "      </Call>\n";
    s += "    </Get>\n";
    s += "  </Get>\n";
    s += "</Configure>\n";
    return s;
  }

}

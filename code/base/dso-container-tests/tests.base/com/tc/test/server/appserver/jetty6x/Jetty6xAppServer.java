package com.tc.test.server.appserver.jetty6x;

import org.apache.commons.io.IOUtils;

import com.tc.process.Exec;
import com.tc.process.HeartBeatService;
import com.tc.process.Exec.Result;
import com.tc.test.TestConfigObject;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AbstractAppServer;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.AppServerResult;
import com.tc.test.server.appserver.cargo.CargoLinkedChildProcess;
import com.tc.test.server.util.AppServerUtil;
import com.tc.util.PortChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
  private final String        STOP_KEY           = "secret";
  private final String        JETTY_MAIN_CLASS   = "org.mortbay.start.Main";
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
    Process stop_cmd = Runtime.getRuntime().exec(cmd, null, this.serverInstallDirectory());
    int exit_code = stop_cmd.waitFor();
    if (exit_code != 0) {
      System.err.println("error stopping isntance " + instanceName);
    }

    if (runner != null) {
      runner.join(START_STOP_TIMEOUT);
    }

    if (runner.isAlive()) {
      System.err.println("Instance " + instanceName + " on port " + jetty_port + " still alive.");
    } else {
      System.err.println("jetty instance " + instanceName + " stopped");
    }
  }

  private AppServerResult startJetty(AppServerParameters params) throws Exception {
    prepareDeployment(params);

    String[] jvmargs = params.jvmArgs().replaceAll("'", "").split("\\s+");
    List cmd = new ArrayList(Arrays.asList(jvmargs));
    cmd.add(0, JAVA_CMD);
    cmd.add("-cp");
    cmd.add(this.serverInstallDirectory() + File.separator + "start.jar" + File.pathSeparator
            + TestConfigObject.getInstance().linkedChildProcessClasspath());
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
    // move wars into the correct location
    Map wars = params.wars();
    if (wars != null && wars.size() > 0) {
      File wars_dir = getWarsDirectory();
      
      Set war_entries = wars.entrySet();
      Iterator war_entries_it = war_entries.iterator();
      while (war_entries_it.hasNext()) {
        Map.Entry war_entry = (Map.Entry)war_entries_it.next();
        File war_file = (File)war_entry.getValue();
        war_file.renameTo(new File(wars_dir, war_file.getName()));
      }
    }
    
    // setup deployment config
    PortChooser portChooser = new PortChooser();
    jetty_port = portChooser.chooseRandomPort();
    stop_port = portChooser.chooseRandomPort();

    instanceName = params.instanceName();
    instanceDir = new File(sandboxDirectory(), instanceName);
    workDir = new File(sandboxDirectory(), "work");
    workDir.mkdirs();
    
    if (new File(instanceDir, "logs").mkdirs() == false) { throw new Exception(
                                                                               "Can't create logs directory for jetty instance: "
                                                                                   + instanceName); }
    setProperties(params, jetty_port, instanceDir);
    createConfigFile();
  }
  
  protected File getWarsDirectory() {
    return new File(this.sandboxDirectory(), "war");
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

      configFile = this.sandboxDirectory().getAbsolutePath() + File.separator + "jetty.xml";
      out = new PrintWriter(new FileWriter(configFile));
      out.println(buffer.toString());

    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }
}

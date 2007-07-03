/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.wasce1x;

import org.apache.commons.io.FileUtils;
import org.codehaus.cargo.container.geronimo.internal.GeronimoUtils;
import org.codehaus.cargo.util.log.Logger;

import com.tc.process.HeartBeatService;
import com.tc.process.StreamAppender;
import com.tc.test.TestConfigObject;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AbstractAppServer;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.AppServerResult;
import com.tc.test.server.appserver.cargo.CargoLinkedChildProcess;
import com.tc.test.server.util.AppServerUtil;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * Wasce1x AppServer implementation
 */
public final class Wasce1xAppServer extends AbstractAppServer {

  private static final String JAVA_CMD             = System.getProperty("java.home") + File.separator + "bin"
                                                     + File.separator + "java";
  private static final String CONFIG_STORE         = "config-store";
  private static final String REPOSITORY           = "repository";
  private static final String VAR                  = "var";
  private static final String BIN                  = "bin" + File.separator;
  private static final String SERVER_JAR           = BIN + "server.jar";
  private static final String SHUTDOWN_JAR         = BIN + "shutdown.jar";
  private static final String DEPLOYER_JAR         = BIN + "deployer.jar";
  private static final String CONFIG_DIR           = VAR + File.separator + "config";
  private static final String CONFIG               = "config.xml";

  private static final String PORT_ATTRIB          = ".*<attribute name=\"port\">\\d{4,6}</attribute>.*";
  private static final String REDIRECT_PORT_ATTRIB = ".*<attribute name=\"redirectPort\">\\d{4,6}</attribute>.*";
  private static final String PORT_PREFIX          = "ort\">";
  private static final String WEB_PORT_ATTRIB      = ".*<gbean name=\"TomcatWebConnector\">.*";
  private static final String RMI_PORT_ATTRIB      = ".*<gbean name=\"RMIRegistry\">.*";
  private static final String RMI_PORT_URL         = ".*<attribute name=\"namingProviderUrl\">rmi://0.0.0.0:\\d{4,6}</attribute>.*";
  private static final String RMI_PREFIX           = "rmi://0.0.0.0:";
  private static final String JMX_RMI              = ".*<gbean name=\"JMXService\">.*";
  private static final String JMX_RMI_PREFIX       = "service:jmx:rmi://0.0.0.0:";
  private static final String INIT_PARAMS          = "<attribute name=\"initParams\">";

  private static final String BASE_DIR_PROP        = "org.apache.geronimo.base.dir";
  private static final String TMP_DIR_PROP         = "java.io.tmpdir";
  private static final String ENDORSED_DIR_PROP    = "java.endorsed.dirs";

  private static final String USERNAME             = "system";
  private static final String PASSWORD             = "manager";

  private static final long   STARTUP_TIMEOUT      = 1000 * 240;

  private String              className, classpath, endorsedPath, installPath;
  private int                 rmiPort;
  private ConsoleLogger       consoleLogger;
  private static final String LOG_CAT              = "WASCE 1.0 STARTUP";
  private String instanceName;

  public Wasce1xAppServer(Wasce1xAppServerInstallation installation) {
    super(installation);
  }

  private File getHome() {
    return serverInstallDirectory();
  }

  public synchronized ServerResult start(ServerParameters rawParams) throws Exception {
    TestConfigObject config = TestConfigObject.getInstance();
    AppServerParameters params = (AppServerParameters) rawParams;
    int port = AppServerUtil.getPort();
    instanceName = params.instanceName();
    final File instance = createInstance(params);
    File home = getHome();
    installPath = home.getCanonicalPath();
    setProperties(params, port, instance);

    interpretJarManifest(new File(home + File.separator + SERVER_JAR));
    copyInstanceDirectories(home, instance);
    parseConfig(new File(instance + File.separator + CONFIG_DIR), port);

    final List cl = new LinkedList();
    cl.add(JAVA_CMD);
    String[] jvmArgs = params.jvmArgs().replaceAll("'", "").split("\\s");
    for (int i = 0; i < jvmArgs.length; i++) {
      if (!("" + jvmArgs[i]).trim().equals("")) cl.add(jvmArgs[i]);
    }

    cl.add("-D" + ENDORSED_DIR_PROP + "=" + endorsedPath);
    cl.add("-D" + TMP_DIR_PROP + "=" + instance.getCanonicalPath() + File.separator + VAR + File.separator + "temp");
    cl.add("-D" + BASE_DIR_PROP + "=" + instance.getCanonicalPath());
    // cl.add("-Xmx128m");
    // cl.add("-verbose:gc");
    cl.add("-classpath");
    cl.add(classpath + File.pathSeparatorChar + config.linkedChildProcessClasspath());
    cl.add(CargoLinkedChildProcess.class.getName());
    cl.add(className);
    cl.add(String.valueOf(HeartBeatService.listenPort()));
    cl.add(instance.toString());
    cl.add("--long"); // wasce args

    consoleLogger = new ConsoleLogger(params.instanceName());
    consoleLogger.info(Arrays.asList(cl.toArray(new String[0])).toString(), LOG_CAT);

    final Logger logger = consoleLogger;
    final String logFileName = new File(instance.getParent(), instance.getName() + ".log").getAbsolutePath();

    Thread t = new Thread() {
      public void run() {
        FileOutputStream logFile = null;
        try {
          logFile = new FileOutputStream(logFileName);
          Process process = Runtime.getRuntime().exec((String[]) cl.toArray(new String[0]), null, instance);
          StreamAppender appender = new StreamAppender(logFile);
          appender.writeInput(process.getErrorStream(), process.getInputStream());
          if (process.waitFor() != 0) logger.warn("Server exited with exit code other than 0", LOG_CAT);
          appender.finish();
        } catch (Exception e) {
          logger.warn("Server process failed", LOG_CAT);
          e.printStackTrace();
        } finally {
          if (logFile != null) {
            try {
              logFile.close();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    };
    t.start();
    waitForStartup(port); // blocking
    deployWars(params.wars());

    return new AppServerResult(port, this);
  }

  private void waitForStartup(int port) throws Exception {
    final ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    final long timeout = System.currentTimeMillis() + STARTUP_TIMEOUT;

    GeronimoUtils utils = new GeronimoUtils();
    ClassLoader geronimoLoader = utils.createGeronimoURLClassloader(getHome());
    Thread.currentThread().setContextClassLoader(geronimoLoader);
    try {
      while (System.currentTimeMillis() < timeout) {
        boolean started = utils.isGeronimoStarted("localhost", String.valueOf(rmiPort), USERNAME, PASSWORD);
        if (started) { return; }
        ThreadUtil.reallySleep(6000);
      }

      throw new Exception("WASCE server failed to start in " + STARTUP_TIMEOUT + " millis");
    } finally {
      Thread.currentThread().setContextClassLoader(prevLoader);
    }
  }

  private void deployWars(Map wars) throws Exception {
    Assert.assertNotNull(wars);

    for (Iterator iter = wars.values().iterator(); iter.hasNext();) {
      List cl = new LinkedList();
      cl.add(JAVA_CMD);
      cl.add("-jar");
      cl.add(installPath + File.separator + DEPLOYER_JAR);
      cl.add("--user");
      cl.add(USERNAME);
      cl.add("--password");
      cl.add(PASSWORD);
      cl.add("--port");
      cl.add(String.valueOf(rmiPort));
      cl.add("deploy");
      cl.add(((File) iter.next()).toString());

      consoleLogger.info("Deploying War: " + Arrays.asList(cl.toArray(new String[0])), LOG_CAT);

      Process process = Runtime.getRuntime().exec((String[]) cl.toArray(new String[0]));
      StreamAppender appender = new StreamAppender(System.err);
      appender.writeInput(process.getErrorStream(), process.getInputStream());
      if (process.waitFor() != 0) throw new Exception("Failed to Deploy WAR: " + rmiPort);
      appender.finish();
    }
  }

  public synchronized void stop() throws Exception {
    Assert.assertTrue(rmiPort > 0);
    StringBuffer cl = new StringBuffer(JAVA_CMD + " -jar ");
    cl.append(installPath + File.separator + SHUTDOWN_JAR);
    cl.append(" --user " + USERNAME + " --password " + PASSWORD + " --port " + rmiPort);
    Process process = Runtime.getRuntime().exec(cl.toString());
    StreamAppender appender = new StreamAppender(System.err);
    appender.writeInput(process.getErrorStream(), process.getInputStream());
    if (process.waitFor() != 0) throw new Exception("Server Shutdown Failed: " + rmiPort);
    appender.finish();
    consoleLogger.info("Server shutdown: " + rmiPort, LOG_CAT);
  }

  private void copyInstanceDirectories(File home, File instance) throws IOException {
    String sep = File.separator;
    FileUtils.copyDirectory(new File(home + sep + CONFIG_STORE), new File(instance + sep + CONFIG_STORE), false);
    FileUtils.copyDirectory(new File(home + sep + REPOSITORY), new File(instance + sep + REPOSITORY), false);
    FileUtils.copyDirectory(new File(home + sep + VAR), new File(instance + sep + VAR), false);
  }

  private void parseConfig(File configDir, int port) throws Exception {
    File tmpConfig = new File(configDir + File.separator + "tmp_config.xml");
    File config = new File(configDir + File.separator + CONFIG);
    BufferedReader reader = new BufferedReader(new FileReader(config));
    PrintWriter writer = new PrintWriter(new FileWriter(tmpConfig));
    String line;
    boolean useServerPort = false;
    boolean useRMIPort = false;

    while ((line = reader.readLine()) != null) {
      if (Pattern.matches(RMI_PORT_ATTRIB, line)) {
        rmiPort = AppServerUtil.getPort();
        useRMIPort = true;
      }
      if (Pattern.matches(INIT_PARAMS, line)) {
        line = "<attribute name=\"initParams\">name=Geronimo jvmRoute=" + instanceName + "</attribute>";
      }
      if (Pattern.matches(WEB_PORT_ATTRIB, line)) useServerPort = true;
      if (Pattern.matches(RMI_PORT_URL, line)) {
        line = line.replaceAll(RMI_PREFIX + "\\d{4,6}", RMI_PREFIX + rmiPort);
      }
      if (Pattern.matches(JMX_RMI, line)) {
        writer.println(line);
        line = reader.readLine();
        String s = "/jndi/rmi://0.0.0.0:";
        line = line.replaceAll(JMX_RMI_PREFIX + "\\d{4,6}" + s + "\\d{4,6}", JMX_RMI_PREFIX + AppServerUtil.getPort()
                                                                             + s + rmiPort);
      }
      if (Pattern.matches(PORT_ATTRIB, line)) {
        int newPort = (useServerPort) ? port : AppServerUtil.getPort();
        if (useRMIPort) {
          newPort = rmiPort;
          useRMIPort = false;
        }
        if (useServerPort) useServerPort = false;
        line = line.replaceAll(PORT_PREFIX + "\\d{4,6}", PORT_PREFIX + newPort);

      } else if (Pattern.matches(REDIRECT_PORT_ATTRIB, line)) {
        line = line.replaceAll(PORT_PREFIX + "\\d{4,6}", PORT_PREFIX + AppServerUtil.getPort());
      }
      writer.println(line);
    }
    reader.close();
    writer.flush();
    writer.close();
    config.delete();
    tmpConfig.renameTo(config);
  }

  private void interpretJarManifest(File jar) throws IOException {
    String absPath = jar.getCanonicalFile().getParentFile().getParent().replace('\\', '/');
    Manifest manifest = new JarFile(jar).getManifest();
    Attributes attrib = manifest.getMainAttributes();
    String classPathAttrib = attrib.getValue("Class-Path");
    classpath = jar + File.pathSeparator;
    classpath += classPathAttrib.replaceAll("^\\.\\.", absPath).replaceAll("\\s\\.\\.",
                                                                           File.pathSeparatorChar + absPath);
    endorsedPath = absPath + File.separator + attrib.getValue("Endorsed-Dirs");
    className = attrib.getValue("Main-Class");
  }
}

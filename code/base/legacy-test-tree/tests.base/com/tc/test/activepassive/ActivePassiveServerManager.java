/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.util.PortChooser;
import com.tctest.TestState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ActivePassiveServerManager {
  private static final String                    HOST             = "localhost";
  private static final String                    SERVER_NAME      = "testserver";
  private static final String                    CONFIG_FILE_NAME = "active-passive-server-config.xml";
  private static final boolean                   DEBUG            = false;
  private static final int                       NULL_VAL         = -1;

  private final File                             tempDir;
  private final PortChooser                      portChooser;
  private final String                           configModel;
  private final ActivePassiveTestSetupManager    setupManger;
  private final long                             startTimeout;

  private final int                              serverCount;
  private final String                           serverCrashMode;
  private final long                             serverCrashWaitTimeInSec;
  private final String                           serverPersistence;
  private final boolean                          serverNetworkShare;
  private final ActivePassiveServerConfigCreator serverConfigCreator;
  private final String                           configFileLocation;
  private final File                             configFile;

  private final ServerInfo[]                     servers;
  private final int[]                            dsoPorts;
  private final int[]                            jmxPorts;
  private final int[]                            l2GroupPorts;
  private final String[]                         serverNames;
  private final TCServerInfoMBean[]              tcServerInfoMBeans;
  private final JMXConnector[]                   jmxConnectors;

  private final List                             errors;

  private int                                    activeIndex      = NULL_VAL;
  private int                                    lastCrashedIndex = NULL_VAL;
  private ActivePassiveServerCrasher             serverCrasher;
  private int                                    maxCrashCount;
  private final TestState                        testState;
  private Random                                 random;
  private long                                   seed;
  private final File                             javaHome;
  private int                                    pid              = -1;

  public ActivePassiveServerManager(boolean isActivePassiveTest, File tempDir, PortChooser portChooser,
                                    String configModel, ActivePassiveTestSetupManager setupManger, long startTimeout,
                                    File javaHome, TestTVSConfigurationSetupManagerFactory configFactory)
      throws Exception {
    if (!isActivePassiveTest) { throw new AssertionError("A non-ActivePassiveTest is trying to use this class."); }

    this.setupManger = setupManger;

    serverCount = this.setupManger.getServerCount();

    if (serverCount < 2) { throw new AssertionError("Active-passive tests involve 2 or more DSO servers: serverCount=["
                                                    + serverCount + "]"); }

    this.tempDir = tempDir;
    configFileLocation = this.tempDir + File.separator + CONFIG_FILE_NAME;
    configFile = new File(configFileLocation);

    this.portChooser = portChooser;
    this.configModel = configModel;
    this.startTimeout = startTimeout * 2;

    serverCrashMode = this.setupManger.getServerCrashMode();
    serverCrashWaitTimeInSec = this.setupManger.getServerCrashWaitTimeInSec();
    maxCrashCount = this.setupManger.getMaxCrashCount();
    serverPersistence = this.setupManger.getServerPersistenceMode();
    serverNetworkShare = this.setupManger.isNetworkShare();

    servers = new ServerInfo[this.serverCount];
    dsoPorts = new int[this.serverCount];
    jmxPorts = new int[this.serverCount];
    l2GroupPorts = new int[this.serverCount];
    serverNames = new String[this.serverCount];
    tcServerInfoMBeans = new TCServerInfoMBean[this.serverCount];
    jmxConnectors = new JMXConnector[this.serverCount];
    createServers();

    serverConfigCreator = new ActivePassiveServerConfigCreator(this.serverCount, dsoPorts, jmxPorts, l2GroupPorts,
                                                               serverNames, serverPersistence, serverNetworkShare,
                                                               this.configModel, configFile, this.tempDir,
                                                               configFactory);
    serverConfigCreator.writeL2Config();

    errors = new ArrayList();
    testState = new TestState();
    this.javaHome = javaHome;

    if (serverCrashMode.equals(ActivePassiveCrashMode.RANDOM_SERVER_CRASH)) {
      SecureRandom srandom = SecureRandom.getInstance("SHA1PRNG");
      seed = srandom.nextLong();
      random = new Random(seed);
      System.out.println("***** Random number generator seed=[" + seed + "]");
    }
  }

  private void resetActiveIndex() {
    activeIndex = NULL_VAL;
  }

  private void resetLastCrashedIndex() {
    lastCrashedIndex = NULL_VAL;
  }

  private void createServers() throws FileNotFoundException {
    int startIndex = 0;

    if (DEBUG) {
      dsoPorts[0] = 8510;
      jmxPorts[0] = 8520;
      l2GroupPorts[0] = 8530;
      serverNames[0] = SERVER_NAME + 0;
      servers[0] = new ServerInfo(HOST, serverNames[0], dsoPorts[0], jmxPorts[0], l2GroupPorts[0],
                                  getServerControl(dsoPorts[0], jmxPorts[0], serverNames[0]));
      dsoPorts[1] = 7510;
      jmxPorts[1] = 7520;
      l2GroupPorts[1] = 7530;
      serverNames[1] = SERVER_NAME + 1;
      servers[1] = new ServerInfo(HOST, serverNames[1], dsoPorts[1], jmxPorts[1], l2GroupPorts[1],
                                  getServerControl(dsoPorts[1], jmxPorts[1], serverNames[1]));
      if (dsoPorts.length > 2) {
        dsoPorts[2] = 6510;
        jmxPorts[2] = 6520;
        l2GroupPorts[2] = 6530;
        serverNames[2] = SERVER_NAME + 2;
        servers[2] = new ServerInfo(HOST, serverNames[2], dsoPorts[2], jmxPorts[2], l2GroupPorts[2],
                                    getServerControl(dsoPorts[2], jmxPorts[2], serverNames[2]));
      }

      startIndex = 3;
    }

    for (int i = startIndex; i < dsoPorts.length; i++) {
      setPorts(i);
      serverNames[i] = SERVER_NAME + i;
      servers[i] = new ServerInfo(HOST, serverNames[i], dsoPorts[i], jmxPorts[i], l2GroupPorts[i],
                                  getServerControl(dsoPorts[i], jmxPorts[i], serverNames[i]));
    }
  }

  private void setPorts(int index) {
    while (true) {
      int newPort = portChooser.chooseRandomPort();
      if (isUnusedPort(newPort)) {
        jmxPorts[index] = newPort;
        break;
      }
    }
    while (true) {
      int newPort = portChooser.chooseRandomPort();
      if (newPort == PortChooser.MAX) {
        continue;
      }
      if (isUnusedPort(newPort) && isUnusedPort(newPort + 1)) {
        dsoPorts[index] = newPort;
        l2GroupPorts[index] = newPort + 1;
        break;
      }
    }
  }

  private boolean isUnusedPort(int port) {
    boolean unused = true;
    for (int i = 0; i < dsoPorts.length; i++) {
      if (dsoPorts[i] == port) {
        unused = false;
      }
    }
    for (int i = 0; i < jmxPorts.length; i++) {
      if (jmxPorts[i] == port) {
        unused = false;
      }
    }
    for (int i = 0; i < l2GroupPorts.length; i++) {
      if (l2GroupPorts[i] == port) {
        unused = false;
      }
    }
    return unused;
  }

  private ServerControl getServerControl(int dsoPort, int jmxPort, String serverName) throws FileNotFoundException {
    List jvmArgs = new ArrayList();
    return new ExtraProcessServerControl(HOST, dsoPort, jmxPort, configFileLocation, true, serverName, jvmArgs,
                                         javaHome, true);
  }

  public void startServers() throws Exception {
    if (activeIndex >= 0) { throw new AssertionError("Server(s) has/have been already started"); }

    for (int i = 0; i < servers.length; i++) {
      servers[i].getServerControl().start(startTimeout);
    }
    Thread.sleep(500 * servers.length);

    debugPrintln("***** startServers():  about to search for active  threadId=[" + Thread.currentThread().getName()
                 + "]");

    for (int i = 0; i < tcServerInfoMBeans.length; i++) {
      debugPrintln("***** Caching tcServerInfoMBean for server=[" + dsoPorts[i] + "]");
      tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
    }

    activeIndex = getActiveIndex();

    if (serverCrashMode.equals(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH)
        || serverCrashMode.equals(ActivePassiveCrashMode.RANDOM_SERVER_CRASH)) {
      startContinuousCrash();
    }
  }

  private void startContinuousCrash() {
    serverCrasher = new ActivePassiveServerCrasher(this, serverCrashWaitTimeInSec, maxCrashCount, testState);
    new Thread(serverCrasher).start();
  }

  public void storeErrors(Exception e) {
    if (e != null) {
      synchronized (errors) {
        errors.add(e);
      }
    }
  }

  public List getErrors() {
    synchronized (errors) {
      List l = new ArrayList();
      l.addAll(errors);
      return l;
    }
  }

  private int getActiveIndex() throws Exception {
    int index = -1;
    while (index < 0) {
      System.out.println("Searching for active server... ");
      for (int i = 0; i < jmxPorts.length; i++) {
        if (i != lastCrashedIndex) {
          if (!servers[i].getServerControl().isRunning()) { throw new AssertionError("Server["
                                                                                     + servers[i].getDsoPort()
                                                                                     + "] is not running as expected!"); }
          boolean isActive;
          try {
            isActive = tcServerInfoMBeans[i].isActive();
          } catch (Exception e) {
            System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[i] + "]...");
            tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
            isActive = tcServerInfoMBeans[i].isActive();
          }
          debugPrintln("********  index=[" + index + "]  i=[" + i + "] active=[" + isActive + "]  threadId=["
                       + Thread.currentThread().getName() + "]");

          if (isActive) {
            if (index < 0) {
              index = i;
              debugPrintln("***** active found index=[" + index + "]");
            } else {
              throw new Exception("More than one active server found.");
            }
          }
        }
      }
      Thread.sleep(1000);
    }
    return index;
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  private void waitForPassive() throws Exception {
    while (true) {
      System.out.println("Searching for appropriate passive server(s)... ");
      for (int i = 0; i < jmxPorts.length; i++) {
        if (i != activeIndex) {
          if (!servers[i].getServerControl().isRunning()) { throw new AssertionError("Server["
                                                                                     + servers[i].getDsoPort()
                                                                                     + "] is not running as expected!"); }
          boolean isPassiveStandby;
          try {
            isPassiveStandby = tcServerInfoMBeans[i].isPassiveStandby();
          } catch (Exception e) {
            System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[i] + "]...");
            tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
            isPassiveStandby = tcServerInfoMBeans[i].isPassiveStandby();
          }
          if (serverNetworkShare && isPassiveStandby) {
            return;
          } else if (!serverNetworkShare && tcServerInfoMBeans[i].isStarted()) {
            return;
          } else if (tcServerInfoMBeans[i].isActive()) { throw new AssertionError(
                                                                                  "Server["
                                                                                      + servers[i].getDsoPort()
                                                                                      + "] is in active mode when it should not be!"); }
        }
      }
      Thread.sleep(1000);
    }
  }

  private TCServerInfoMBean getTcServerInfoMBean(int index) throws IOException {
    jmxConnectors[index] = getJMXConnector(jmxPorts[index]);
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    return (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                                             TCServerInfoMBean.class, true);
  }

  public static JMXConnector getJMXConnector(int jmxPort) throws IOException {
    String url = "service:jmx:rmi:///jndi/rmi://" + HOST + ":" + jmxPort + "/jmxrmi";
    JMXServiceURL jmxServerUrl = new JMXServiceURL(url);
    JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(jmxServerUrl, null);
    jmxConnector.connect();
    return jmxConnector;
  }

  public void stopAllServers() throws Exception {
    synchronized (testState) {
      debugPrintln("***** setting TestState to STOPPING");
      testState.setTestState(TestState.STOPPING);

      closeJMXConnectors();

      for (int i = 0; i < serverCount; i++) {
        debugPrintln("***** stopping server=[" + servers[i].getDsoPort() + "]");
        ServerControl sc = servers[i].getServerControl();

        if (!sc.isRunning()) {
          if (i == lastCrashedIndex) {
            continue;
          } else {
            throw new AssertionError("Server[" + servers[i].getDsoPort() + "] is not running as expected!");
          }
        }

        if (i == activeIndex) {
          sc.shutdown();
          continue;
        }

        try {
          sc.crash();
        } catch (Exception e) {
          if (DEBUG) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public void dumpAllServers(int currentPid, int dumpCount, long dumpInterval) throws Exception {
    pid = currentPid;
    for (int i = 0; i < serverCount; i++) {
      if (!serverNetworkShare && i != activeIndex) {
        debugPrintln("***** skipping dumping server=[" + dsoPorts[i] + "]");
        continue;
      }
      if (servers[i].getServerControl().isRunning()) {
        System.out.println("Dumping server=[" + dsoPorts[i] + "]");

        MBeanServerConnection mbs;
        try {
          mbs = jmxConnectors[i].getMBeanServerConnection();
        } catch (IOException ioe) {
          System.out.println("Need to recreate jmxConnector for server=[" + dsoPorts[i] + "]...");
          jmxConnectors[i] = getJMXConnector(jmxPorts[i]);
          mbs = jmxConnectors[i].getMBeanServerConnection();
        }
        L2DumperMBean mbean = (L2DumperMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                                            L2DumperMBean.class, true);
        mbean.doServerDump();
        if (pid != 0) {
          mbean.setThreadDumpCount(dumpCount);
          mbean.setThreadDumpInterval(dumpInterval);
          System.out.println("Thread dumping server=[" + dsoPorts[i] + "] pid=[" + pid + "]");
          pid = mbean.doThreadDump();
        }
      }
    }
    closeJMXConnectors();
  }

  private void closeJMXConnectors() {
    for (int i = 0; i < jmxConnectors.length; i++) {
      try {
        jmxConnectors[i].close();
      } catch (Exception e) {
        System.out.println("JMXConnector for server=[" + dsoPorts[i] + "] already closed.");
        e.printStackTrace();
      }
    }
  }

  public int getPid() {
    return pid;
  }

  public void crashActive() throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping crash active");
      return;
    }

    if (activeIndex < 0) { throw new AssertionError("Active index was not set."); }

    System.out.println("Crashing active server: dsoPort=[" + servers[activeIndex].getDsoPort() + "]");

    debugPrintln("***** wait to find an appropriate passive server.");
    waitForPassive();
    debugPrintln("***** finished waiting to find an appropriate passive server.");

    verifyActiveServerState();
    debugPrintln("***** Closing active's jmxConnector ");
    jmxConnectors[activeIndex].close();
    ServerControl server = servers[activeIndex].getServerControl();
    server.crash();
    debugPrintln("***** Sleeping after crashing active server ");
    waitForServerCrash(server);
    debugPrintln("***** Done sleeping after crashing active server ");

    lastCrashedIndex = activeIndex;
    resetActiveIndex();
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");

    debugPrintln("***** about to search for active  threadId=[" + Thread.currentThread().getName() + "]");
    activeIndex = getActiveIndex();
    debugPrintln("***** activeIndex[" + activeIndex + "] ");
  }

  private void verifyActiveServerState() throws Exception {
    ServerControl server = servers[activeIndex].getServerControl();
    if (!server.isRunning()) { throw new AssertionError("Server[" + servers[activeIndex].getDsoPort()
                                                        + "] is not running as expected!"); }
    JMXConnector jmxConnector = getJMXConnector(jmxPorts[activeIndex]);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    TCServerInfoMBean mbean = (TCServerInfoMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, true);
    if (!mbean.isActive()) {
      jmxConnector.close();
      throw new AssertionError("Server[" + servers[activeIndex].getDsoPort() + "] is not an active server as expected!");
    }
    jmxConnector.close();
  }

  private void waitForServerCrash(ServerControl server) throws Exception {
    long duration = 10000;
    long startTime = System.currentTimeMillis();
    while (duration > (System.currentTimeMillis() - startTime)) {
      if (server.isRunning()) {
        Thread.sleep(1000);
      } else {
        return;
      }
    }
    throw new Exception("Server crash did not complete.");
  }

  private void crashPassive(int passiveToCrash) throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping crash passive");
      return;
    }

    System.out.println("Crashing passive server: dsoPort=[" + servers[passiveToCrash].getDsoPort() + "]");

    debugPrintln("***** Closing passive's jmxConnector ");
    jmxConnectors[passiveToCrash].close();

    ServerControl server = servers[passiveToCrash].getServerControl();
    if (!server.isRunning()) { throw new AssertionError("Server[" + servers[passiveToCrash].getDsoPort()
                                                        + "] is not running as expected!"); }
    server.crash();
    debugPrintln("***** Sleeping after crashing passive server ");
    waitForServerCrash(server);
    debugPrintln("***** Done sleeping after crashing passive server ");

    lastCrashedIndex = passiveToCrash;
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");
  }

  private void crashRandomServer() throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping crash random server");
      return;
    }

    if (activeIndex < 0) { throw new AssertionError("Active index was not set."); }
    if (random == null) { throw new AssertionError("Random number generator was not set."); }

    debugPrintln("***** Choosing random server... ");

    int crashIndex = random.nextInt(serverCount);

    if (crashIndex == activeIndex) {
      crashActive();
    } else {
      crashPassive(crashIndex);
    }
  }

  public void restartLastCrashedServer() throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping restart");
      return;
    }

    debugPrintln("*****  restarting crashed server");

    if (lastCrashedIndex >= 0) {
      if (servers[lastCrashedIndex].getServerControl().isRunning()) { throw new AssertionError(
                                                                                               "Server["
                                                                                                   + servers[lastCrashedIndex]
                                                                                                       .getDsoPort()
                                                                                                   + "] is not down as expected!"); }
      servers[lastCrashedIndex].getServerControl().start(startTimeout);

      if (!servers[lastCrashedIndex].getServerControl().isRunning()) { throw new AssertionError(
                                                                                                "Server["
                                                                                                    + servers[lastCrashedIndex]
                                                                                                        .getDsoPort()
                                                                                                    + "] is not running as expected!"); }
      resetLastCrashedIndex();
    } else {
      throw new AssertionError("No crashed servers to restart.");
    }
  }

  public int getServerCount() {
    return serverCount;
  }

  public int[] getDsoPorts() {
    return dsoPorts;
  }

  public int[] getJmxPorts() {
    return jmxPorts;
  }

  public boolean crashActiveServerAfterMutate() {
    if (serverCrashMode.equals(ActivePassiveCrashMode.CRASH_AFTER_MUTATE)) { return true; }
    return false;
  }

  public void addServersToL1Config(TestTVSConfigurationSetupManagerFactory configFactory) {
    for (int i = 0; i < serverCount; i++) {

      debugPrintln("******* adding to L1 config: serverName=[" + serverNames[i] + "] dsoPort=[" + dsoPorts[i]
                   + "] jmxPort=[" + jmxPorts[i] + "]");

      configFactory.addServerToL1Config(serverNames[i], dsoPorts[i], jmxPorts[i]);
    }
  }

  public void crashServer() throws Exception {
    if (serverCrashMode.equals(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH)) {
      crashActive();
    } else if (serverCrashMode.equals(ActivePassiveCrashMode.RANDOM_SERVER_CRASH)) {
      crashRandomServer();
    }
  }

  /*
   * Server inner class
   */
  private static class ServerInfo {
    private final String        server_host;
    private final String        server_name;
    private final int           server_dsoPort;
    private final int           server_jmxPort;
    private final int           server_l2GroupPort;
    private final ServerControl serverControl;
    private String              dataLocation;
    private String              logLocation;

    ServerInfo(String host, String name, int dsoPort, int jmxPort, int l2GroupPort, ServerControl serverControl) {
      server_host = host;
      server_name = name;
      server_dsoPort = dsoPort;
      server_jmxPort = jmxPort;
      server_l2GroupPort = l2GroupPort;
      this.serverControl = serverControl;
    }

    public String getHost() {
      return server_host;
    }

    public String getName() {
      return server_name;
    }

    public int getDsoPort() {
      return server_dsoPort;
    }

    public int getJmxPort() {
      return server_jmxPort;
    }

    public int getL2GroupPort() {
      return server_l2GroupPort;
    }

    public ServerControl getServerControl() {
      return serverControl;
    }

    public void setDataLocation(String location) {
      dataLocation = location;
    }

    public String getDataLocation() {
      return dataLocation;
    }

    public void setLogLocation(String location) {
      logLocation = location;
    }

    public String getLogLocation() {
      return logLocation;
    }
  }

}

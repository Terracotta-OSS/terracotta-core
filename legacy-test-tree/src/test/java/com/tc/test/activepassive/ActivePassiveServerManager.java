/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.stats.api.DGCMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.GroupData;
import com.tc.test.JMXUtils;
import com.tc.test.MultipleServerManager;
import com.tc.test.MultipleServersConfigCreator;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TestState;
import com.terracottatech.config.Servers;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ActivePassiveServerManager extends MultipleServerManager {
  private static final String        HOST                = "localhost";
  private static final String        SERVER_NAME         = "testserver";
  private static final boolean       DEBUG               = false;
  private static final int           NULL_VAL            = -1;

  private final File                 tempDir;
  private final PortChooser          portChooser;
  private final String               configModel;

  private final int                  serverCount;
  private final String               serverCrashMode;
  private final long                 serverCrashWaitTimeInSec;
  private final String               serverPersistence;
  private final boolean              serverNetworkShare;
  private final String               configFileLocation;
  private final File                 configFile;

  private final ServerInfo[]         servers;
  private final int[]                dsoPorts;
  private final int[]                jmxPorts;
  private final int[]                l2GroupPorts;
  private final String[]             serverNames;
  private final TCServerInfoMBean[]  tcServerInfoMBeans;
  private final JMXConnector[]       jmxConnectors;

  private final List                 errors;

  private int                        activeIndex         = NULL_VAL;
  private int                        lastCrashedIndex    = NULL_VAL;
  private ActivePassiveServerCrasher serverCrasher;
  private final int                  maxCrashCount;
  private final TestState            testState;
  private Random                     random;
  private long                       seed;
  private final File                 javaHome;
  private int                        pid                 = -1;
  private List                       jvmArgs             = null;
  private final boolean              isProxyL2groupPorts;
  private final int[]                proxyL2GroupPorts;
  protected ProxyConnectManager[]    proxyL2Managers;

  private final boolean              isProxyDsoPorts;
  private final int[]                proxyDsoPorts;
  protected ProxyConnectManager[]    proxyL1Managers;

  // this is used when active-active tests are run. This will help in differentiating between the names in the different
  // groups
  private int                        startIndexOfServer  = 0;
  private final String               groupName;
  private static final String        ACTIVEPASSIVE_GROUP = "active-passive-group";

  // Should be called directly when an active-passive test is to be run.
  public ActivePassiveServerManager(boolean isActivePassiveTest, File tempDir, PortChooser portChooser,
                                    String configModel, MultipleServersTestSetupManager setupManger, File javaHome,
                                    TestConfigurationSetupManagerFactory configFactory, List extraJvmArgs,
                                    boolean isProxyL2GroupPorts, boolean isProxyDsoPorts,
                                    DSOApplicationConfigBuilder dsoApplicationBuilder) throws Exception {
    this(ACTIVEPASSIVE_GROUP, isActivePassiveTest, tempDir, portChooser, configModel, setupManger, javaHome,
         configFactory, extraJvmArgs, isProxyL2GroupPorts, isProxyDsoPorts, false, 0, dsoApplicationBuilder);
  }

  // Should be called directly when an active-active test is to be run. In case of active active config is not written
  public ActivePassiveServerManager(String groupName, boolean isActivePassiveTest, File tempDir,
                                    PortChooser portChooser, String configModel,
                                    MultipleServersTestSetupManager setupManger, File javaHome,
                                    TestConfigurationSetupManagerFactory configFactory, List extraJvmArgs,
                                    boolean isProxyL2GroupPorts, boolean isProxyDsoPorts, boolean isActiveActive,
                                    int startIndexOfServer, DSOApplicationConfigBuilder dsoApplicationBuilder)
      throws Exception {
    super(setupManger);

    this.groupName = groupName;
    this.isProxyL2groupPorts = isProxyL2GroupPorts;
    this.isProxyDsoPorts = isProxyDsoPorts;
    this.jvmArgs = extraJvmArgs;

    if (!isActivePassiveTest) { throw new AssertionError("A non-ActivePassiveTest is trying to use this class."); }

    serverCount = this.setupManger.getServerCount();

    if (serverCount < 2 && !isActiveActive) { throw new AssertionError(
                                                                       "Active-passive tests involve 2 or more DSO servers: serverCount=["
                                                                           + serverCount + "]"); }

    this.tempDir = tempDir;
    configFileLocation = this.tempDir + File.separator + CONFIG_FILE_NAME;
    configFile = new File(configFileLocation);

    this.portChooser = portChooser;
    this.configModel = configModel;

    serverCrashMode = this.setupManger.getServerCrashMode();
    serverCrashWaitTimeInSec = this.setupManger.getServerCrashWaitTimeInSec();
    maxCrashCount = this.setupManger.getMaxCrashCount();
    serverPersistence = this.setupManger.getServerPersistenceMode();
    serverNetworkShare = this.setupManger.isNetworkShare();

    servers = new ServerInfo[this.serverCount];
    dsoPorts = new int[this.serverCount];
    jmxPorts = new int[this.serverCount];
    l2GroupPorts = new int[this.serverCount];
    proxyL2GroupPorts = new int[this.serverCount];
    proxyDsoPorts = new int[this.serverCount];
    serverNames = new String[this.serverCount];
    tcServerInfoMBeans = new TCServerInfoMBean[this.serverCount];
    jmxConnectors = new JMXConnector[this.serverCount];
    this.startIndexOfServer = startIndexOfServer;
    createServers(this.startIndexOfServer);

    if (!isActiveActive) {
      GroupData[] groupList = new GroupData[1];
      groupList[0] = new GroupData(ACTIVEPASSIVE_GROUP, dsoPorts, jmxPorts, (isProxyL2GroupPorts) ? proxyL2GroupPorts
          : l2GroupPorts, serverNames);
      MultipleServersConfigCreator serversConfigCreator = new MultipleServersConfigCreator(this.setupManger, groupList,
                                                                                           this.configModel,
                                                                                           configFile, this.tempDir,
                                                                                           configFactory,
                                                                                           dsoApplicationBuilder);
      this.serverConfigCreator = serversConfigCreator;
      serverConfigCreator.writeL2Config();
    }

    // setup proxy
    if (isProxyL2GroupPorts) {
      proxyL2Managers = new ProxyConnectManager[this.serverCount];
      for (int i = 0; i < this.serverCount; ++i) {
        proxyL2Managers[i] = new ProxyConnectManagerImpl();
        proxyL2Managers[i].setDsoPort(l2GroupPorts[i]);
        proxyL2Managers[i].setProxyPort(proxyL2GroupPorts[i]);
        proxyL2Managers[i].setupProxy();
      }
    }

    if (isProxyDsoPorts) {
      proxyL1Managers = new ProxyConnectManager[this.serverCount];
      for (int i = 0; i < this.serverCount; ++i) {
        proxyL1Managers[i] = new ProxyConnectManagerImpl();
        proxyL1Managers[i].setDsoPort(dsoPorts[i]);
        proxyL1Managers[i].setProxyPort(proxyDsoPorts[i]);
        proxyL1Managers[i].setupProxy();
      }
    }

    errors = new ArrayList();
    testState = new TestState();
    this.javaHome = javaHome;

    if (serverCrashMode.equals(MultipleServersCrashMode.RANDOM_SERVER_CRASH)) {
      SecureRandom srandom = SecureRandom.getInstance("SHA1PRNG");
      seed = srandom.nextLong();
      random = new Random(seed);
      System.out.println("***** Random number generator seed=[" + seed + "]");
    }
  }

  public void setConfigCreator(MultipleServersConfigCreator creator) {
    if (this.serverConfigCreator != null) { throw new AssertionError(
                                                                     "MultipleServersConfigCreator should not be created again"); }
    this.serverConfigCreator = creator;
  }

  public MultipleServersConfigCreator getConfigCreator() {
    return this.serverConfigCreator;
  }

  @Override
  public ProxyConnectManager[] getL2ProxyManagers() {
    return proxyL2Managers;
  }

  private void resetActiveIndex() {
    activeIndex = NULL_VAL;
  }

  private void resetLastCrashedIndex() {
    lastCrashedIndex = NULL_VAL;
  }

  private void createServers(int serverNameStartIndex) {
    int startIndex = 0;
    if (DEBUG) {
      dsoPorts[0] = 8510;
      jmxPorts[0] = 8520;
      l2GroupPorts[0] = 8530;
      serverNames[0] = SERVER_NAME + (serverNameStartIndex + 0);
      servers[0] = new ServerInfo(dsoPorts[0], getServerControl(dsoPorts[0], jmxPorts[0], serverNames[0]));
      dsoPorts[1] = 7510;
      jmxPorts[1] = 7520;
      l2GroupPorts[1] = 7530;
      serverNames[1] = SERVER_NAME + (serverNameStartIndex + 1);
      servers[1] = new ServerInfo(dsoPorts[1], getServerControl(dsoPorts[1], jmxPorts[1], serverNames[1]));
      if (dsoPorts.length > 2) {
        dsoPorts[2] = 6510;
        jmxPorts[2] = 6520;
        l2GroupPorts[2] = 6530;
        serverNames[2] = SERVER_NAME + (serverNameStartIndex + 2);
        servers[2] = new ServerInfo(dsoPorts[2], getServerControl(dsoPorts[2], jmxPorts[2], serverNames[2]));
      }

      startIndex = 3;
      serverNameStartIndex += 3;
    }

    for (int i = startIndex; i < dsoPorts.length; i++) {
      setPorts(i);
      serverNames[i] = SERVER_NAME + (serverNameStartIndex + i);
      List perServerJvmArgs;
      if (isProxyL2groupPorts) {
        // hidden tc.properties only used by L2 proxy testing purpose
        perServerJvmArgs = new ArrayList(jvmArgs);
        perServerJvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT + "="
                             + l2GroupPorts[i]);
      } else {
        perServerJvmArgs = jvmArgs;
      }

      servers[i] = new ServerInfo(dsoPorts[i], getServerControl(dsoPorts[i], jmxPorts[i], serverNames[i],
                                                                perServerJvmArgs));
    }
  }

  private void setPorts(int serverIndex) {
    while (true) {
      int newPort = portChooser.chooseRandomPort();
      if (isUnusedPort(newPort)) {
        jmxPorts[serverIndex] = newPort;
        break;
      }
    }
    while (true) {
      final int numOfPorts = 4;
      int newPort = portChooser.chooseRandomPorts(numOfPorts);
      if (isUnusedPort(newPort) && isUnusedPort(newPort + 1) && isUnusedPort(newPort + 2) && isUnusedPort(newPort + 3)) {
        dsoPorts[serverIndex] = newPort;
        l2GroupPorts[serverIndex] = newPort + 1;
        proxyL2GroupPorts[serverIndex] = newPort + 2;
        proxyDsoPorts[serverIndex] = newPort + 3;
        break;
      }
    }
  }

  @Override
  public int getDsoPort() {
    return dsoPorts[0];
  }

  @Override
  public int getJMXPort() {
    return jmxPorts[0];
  }

  @Override
  public int getL2GroupPort() {
    return l2GroupPorts[0];
  }

  private boolean isUnusedPort(int port) {
    boolean unused = true;
    for (int dsoPort : dsoPorts) {
      if (dsoPort == port) {
        unused = false;
      }
    }
    for (int jmxPort : jmxPorts) {
      if (jmxPort == port) {
        unused = false;
      }
    }
    for (int l2GroupPort : l2GroupPorts) {
      if (l2GroupPort == port) {
        unused = false;
      }
    }
    for (int proxyL2GroupPort : proxyL2GroupPorts) {
      if (proxyL2GroupPort == port) {
        unused = false;
      }
    }
    for (int proxyDsoPort : proxyDsoPorts) {
      if (proxyDsoPort == port) {
        unused = false;
      }
    }
    return unused;
  }

  private ServerControl getServerControl(int dsoPort, int jmxPort, String serverName) {
    return new ExtraProcessServerControl(HOST, dsoPort, jmxPort, configFileLocation, true, serverName, this.jvmArgs,
                                         javaHome, true, tempDir);
  }

  private ServerControl getServerControl(int dsoPort, int jmxPort, String serverName, List aJvmArgs) {
    return new ExtraProcessServerControl(HOST, dsoPort, jmxPort, configFileLocation, true, serverName, aJvmArgs,
                                         javaHome, true, tempDir);
  }

  public void startServer(int index) throws Exception {
    System.out.println("*** Starting server [" + servers[index].getDsoPort() + "] ... ");
    servers[index].getServerControl().start();
    if (isProxyL2groupPorts) {
      proxyL2Managers[index].proxyUp();
      proxyL2Managers[index].startProxyTest();

      debugPrintln("***** Caching tcServerInfoMBean for server=[" + dsoPorts[index] + "]");
      tcServerInfoMBeans[index] = getTcServerInfoMBean(index);

    }

    System.out.println("*** Server started [" + servers[index].getDsoPort() + "]");
  }

  public int getAndUpdateActiveIndex() throws Exception {
    return (activeIndex = getActiveIndex(false));
  }

  public boolean waitServerIsPassiveStandby(int index, int waitSeconds) throws Exception {
    boolean isPassiveStandby = false;
    while (--waitSeconds > 0) {
      System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[index] + "]...");
      tcServerInfoMBeans[index] = getTcServerInfoMBean(index);
      isPassiveStandby = tcServerInfoMBeans[index].isPassiveStandby();
      if (isPassiveStandby) break;
      Thread.sleep(1000);
    }
    System.out.println("********  index=[" + index + "]  i=[" + index + "] isPassiveStandby=[" + isPassiveStandby
                       + "]  threadId=[" + Thread.currentThread().getName() + "]");
    return isPassiveStandby;
  }

  public void startActivePassiveServers() throws Exception {
    if (activeIndex >= 0) { throw new AssertionError("Server(s) has/have been already started"); }

    for (int i = 0; i < servers.length; i++) {
      startServer(i);
    }
    Thread.sleep(500 * servers.length);

    debugPrintln("***** startServers():  about to search for active  threadId=[" + Thread.currentThread().getName()
                 + "]");

    activeIndex = getActiveIndex(true);

    if (serverCrashMode.equals(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH)
        || serverCrashMode.equals(MultipleServersCrashMode.RANDOM_SERVER_CRASH)) {
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

  @Override
  public List getErrors() {
    synchronized (errors) {
      List l = new ArrayList();
      l.addAll(errors);
      return l;
    }
  }

  private int getActiveIndex(boolean allMustBeRunning) throws Exception {
    int index = -1;
    while (index < 0) {
      System.out.println("Searching for active server... ");
      for (int i = 0; i < jmxPorts.length; i++) {
        if (i != lastCrashedIndex) {
          if (!servers[i].getServerControl().isRunning()) {
            if (allMustBeRunning) {
              throw new AssertionError("Server[" + servers[i].getDsoPort() + "] is not running as expected!");
            } else {
              continue;
            }
          }
          boolean isActive;
          try {
            isActive = tcServerInfoMBeans[i].isActive();
          } catch (Exception e) {
            System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[i] + "]...");
            try {
              tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
              isActive = tcServerInfoMBeans[i].isActive();
            } catch (Exception e2) {
              System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
              continue;
            }
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

    if (isProxyDsoPorts) {
      System.out.println("*** Starting the DSO proxy with proxy port as " + proxyL1Managers[index].getProxyPort()
                         + " and DSO port as " + proxyL1Managers[index].getDsoPort());
      proxyL1Managers[index].proxyUp();
      proxyL1Managers[index].startProxyTest();
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

            if (serverNetworkShare && isPassiveStandby) {
              return;
            } else if (!serverNetworkShare && tcServerInfoMBeans[i].isStarted()) {
              return;
            } else if (tcServerInfoMBeans[i].isActive()) { throw new AssertionError(
                                                                                    "Server["
                                                                                        + servers[i].getDsoPort()
                                                                                        + "] is in active mode when it should not be!"); }
          } catch (Exception e) {
            System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[i] + "]... [" + e.getMessage()
                               + "]");
            try {
              tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
              isPassiveStandby = tcServerInfoMBeans[i].isPassiveStandby();
            } catch (Exception e2) {
              System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
            }
          }
        }
      }
      Thread.sleep(1000);
    }
  }

  private TCServerInfoMBean getTcServerInfoMBean(int index) throws IOException {
    if (jmxConnectors[index] != null) {
      closeJMXConnector(index);
    }
    jmxConnectors[index] = getJMXConnector(jmxPorts[index]);
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                         TCServerInfoMBean.class, true);
  }

  public static JMXConnector getJMXConnector(int jmxPort) throws IOException {
    return JMXUtils.getJMXConnector("localhost", jmxPort);
  }

  public DSOMBean getDsoMBean(int index) throws IOException {
    JMXConnector jmxc = getJMXConnector(jmxPorts[index]);
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DSOMBean dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    return dsoMBean;
  }

  public DGCMBean getLocalDGCMBean(int index) throws IOException {
    JMXConnector jmxc = getJMXConnector(jmxPorts[index]);
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DGCMBean dgcMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.LOCAL_DGC_STATS,
                                                                      DGCMBean.class, false);
    return dgcMBean;
  }

  public void stopServer(int index) throws Exception {

    System.out.println("*** stopping server [" + servers[index].getDsoPort() + "]");
    ServerControl sc = servers[index].getServerControl();

    if (!sc.isRunning()) {
      if (index == lastCrashedIndex) {
        System.out.println("*** Server stopped [" + servers[index].getDsoPort() + "] due to last crashed");
        return;
      } else {
        if (serverCrashMode.equals(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH)) {
          // Customized tests verify by its own.
          return;
        }
        throw new AssertionError("Server[" + servers[index].getDsoPort() + "] is not running as expected!");
      }
    }

    if (index == activeIndex) {
      sc.shutdown();
      if (isProxyL2groupPorts) proxyL2Managers[index].proxyDown();
      if (isProxyDsoPorts) proxyL1Managers[index].proxyDown();

      System.out.println("*** Server(active) stopped [" + servers[index].getDsoPort() + "]");
      return;
    }

    try {
      sc.crash();
      if (isProxyL2groupPorts) proxyL2Managers[index].proxyDown();
    } catch (Exception e) {
      if (DEBUG) {
        e.printStackTrace();
      }
    }
    System.out.println("*** Server stopped [" + servers[index].getDsoPort() + "]");
  }

  @Override
  public void stopAllServers() throws Exception {
    synchronized (testState) {
      debugPrintln("***** setting TestState to STOPPING");
      testState.setTestState(TestState.STOPPING);

      closeJMXConnectors();

      for (int i = 0; i < serverCount; i++) {
        stopServer(i);
      }
    }
  }

  @Override
  public boolean dumpClusterState(int currentPid, int dumpCount, long dumpInterval) throws Exception {
    if (serverCrasher != null) {
      this.serverCrasher.stop();
    }
    pid = currentPid;
    boolean dumpTaken = false;
    getActiveIndex(false);
    for (int i = 0; i < serverCount; i++) {
      if (!serverNetworkShare && i != activeIndex) {
        debugPrintln("***** skipping dumping server=[" + dsoPorts[i] + "]");
        continue;
      }

      if (servers[i].getServerControl().isRunning() && i == activeIndex) {
        System.out.println("Dumping server=[" + dsoPorts[i] + "]");

        MBeanServerConnection mbs;
        try {
          if (jmxConnectors[i] == null) {
            jmxConnectors[i] = getJMXConnector(jmxPorts[i]);
          }
          mbs = jmxConnectors[i].getMBeanServerConnection();
        } catch (IOException ioe) {
          System.out.println("Need to recreate jmxConnector for server=[" + dsoPorts[i] + "]...");
          jmxConnectors[i] = getJMXConnector(jmxPorts[i]);
          mbs = jmxConnectors[i].getMBeanServerConnection();
        }

        L2DumperMBean mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                            L2DumperMBean.class, true);
        mbean.dumpClusterState();

        dumpTaken = true;
        if (pid != 0) {
          mbean.setThreadDumpCount(dumpCount);
          mbean.setThreadDumpInterval(dumpInterval);
          System.out.println("Thread dumping server=[" + dsoPorts[i] + "] pid=[" + pid + "]");
          pid = mbean.doThreadDump();
        }
      }
    }
    closeJMXConnectors();
    return dumpTaken;
  }

  private void closeJMXConnector(int i) {
    if (jmxConnectors[i] != null) {
      try {
        jmxConnectors[i].close();
      } catch (Exception e) {
        System.out.println("JMXConnector for server=[" + dsoPorts[i] + "] already closed.");
        e.printStackTrace();
      }
      jmxConnectors[i] = null;
    }
  }

  private void closeJMXConnectors() {
    for (int i = 0; i < jmxConnectors.length; i++) {
      closeJMXConnector(i);
      ThreadUtil.reallySleep(100);
    }
  }

  @Override
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
    if (serverCount > 1) waitForPassive();
    debugPrintln("***** finished waiting to find an appropriate passive server.");

    verifyActiveServerState();

    ServerControl server = servers[activeIndex].getServerControl();
    server.crash();
    debugPrintln("***** Sleeping after crashing active server ");
    waitForServerCrash(server);
    if (isProxyL2groupPorts) proxyL2Managers[activeIndex].proxyDown();
    if (isProxyDsoPorts) {
      proxyL1Managers[activeIndex].proxyDown();
      System.out.println("*** Stopping the DSO proxy with proxy port as " + proxyL1Managers[activeIndex].getProxyPort()
                         + " and DSO port as " + proxyL1Managers[activeIndex].getDsoPort());
      proxyL1Managers[activeIndex].stopProxyTest();
    }
    debugPrintln("***** Done sleeping after crashing active server ");

    lastCrashedIndex = activeIndex;
    resetActiveIndex();
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");

    debugPrintln("***** about to search for active  threadId=[" + Thread.currentThread().getName() + "]");
    if (serverCount > 1) {
      activeIndex = getActiveIndex(true);
    }
    debugPrintln("***** activeIndex[" + activeIndex + "] ");
  }

  private void verifyActiveServerState() throws Exception {
    ServerControl server = servers[activeIndex].getServerControl();
    if (!server.isRunning()) { throw new AssertionError("Server[" + servers[activeIndex].getDsoPort()
                                                        + "] is not running as expected!"); }

    if (jmxConnectors[activeIndex] == null) {
      jmxConnectors[activeIndex] = getJMXConnector(jmxPorts[activeIndex]);
    }
    MBeanServerConnection mbs = jmxConnectors[activeIndex].getMBeanServerConnection();
    TCServerInfoMBean mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.TC_SERVER_INFO,
                                                                            TCServerInfoMBean.class, true);
    if (!mbean.isActive()) {
      closeJMXConnector(activeIndex);
      throw new AssertionError("Server[" + servers[activeIndex].getDsoPort() + "] is not an active server as expected!");
    }
    closeJMXConnector(activeIndex);
  }

  private void waitForServerCrash(ServerControl server) throws Exception {
    long duration = 10000;
    long startTime = System.currentTimeMillis();
    while (duration > (System.currentTimeMillis() - startTime)) {
      if (server.isRunning()) {
        try {
          Thread.sleep(1000);
        } catch (Exception e) {/**/
        }
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
    closeJMXConnector(passiveToCrash);

    ServerControl server = servers[passiveToCrash].getServerControl();
    if (!server.isRunning()) { throw new AssertionError("Server[" + servers[passiveToCrash].getDsoPort()
                                                        + "] is not running as expected!"); }
    server.crash();
    debugPrintln("***** Sleeping after crashing passive server ");
    waitForServerCrash(server);
    if (isProxyL2groupPorts) proxyL2Managers[passiveToCrash].proxyDown();
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
      servers[lastCrashedIndex].getServerControl().start();
      if (isProxyL2groupPorts) proxyL2Managers[lastCrashedIndex].proxyUp();

      if (!servers[lastCrashedIndex].getServerControl().isRunning()) { throw new AssertionError(
                                                                                                "Server["
                                                                                                    + servers[lastCrashedIndex]
                                                                                                        .getDsoPort()
                                                                                                    + "] is not running as expected!"); }
      resetLastCrashedIndex();
      if (serverCount == 1) {
        activeIndex = getActiveIndex(true);
      }
    } else {
      throw new AssertionError("No crashed servers to restart.");
    }
  }

  public int getServerCount() {
    return serverCount;
  }

  public String getGroupName() {
    return groupName;
  }

  public int[] getDsoPorts() {
    return dsoPorts;
  }

  public int[] getJmxPorts() {
    return jmxPorts;
  }

  public String[] getServerNames() {
    return serverNames;
  }

  public int[] getL2GroupPorts() {
    return isProxyL2groupPorts ? proxyL2GroupPorts : l2GroupPorts;
  }

  public void addServersAndGroupToL1Config(TestConfigurationSetupManagerFactory configFactory, Servers serversForL1) {
    Servers serversCopy = (Servers) serversForL1.copy();
    configFactory.addServersAndGroupToL1Config(serversCopy);
    for (int i = 0; i < serverCount; i++) {

      debugPrintln("******* adding to L1 config: serverName=[" + serverNames[i] + "] dsoPort=["
                   + (isProxyDsoPorts ? proxyDsoPorts[i] : dsoPorts[i]) + "] jmxPort=[" + jmxPorts[i] + "]");
      if (isProxyDsoPorts) {
        serversCopy.getServerArray(i).getDsoPort().setIntValue(proxyDsoPorts[i]);
      }
    }
  }

  public void crashServer() throws Exception {
    if (serverCrashMode.equals(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH)) {
      crashActive();
    } else if (serverCrashMode.equals(MultipleServersCrashMode.RANDOM_SERVER_CRASH)) {
      crashRandomServer();
    }

    if (serverCount > 1) {
      cleanupServerDB(lastCrashedIndex);
    }
  }

  public void cleanupServerDB(int index) throws Exception {
    if (serverNetworkShare && serverPersistence.equals(MultipleServersPersistenceMode.PERMANENT_STORE)) {
      System.out.println("Deleting data directory for server=[" + servers[index].getDsoPort() + "]");
      deleteDirectory(serverConfigCreator.getDataLocation(startIndexOfServer + index));
    }
  }

  private void deleteDirectory(String directory) {
    debugPrintln("\n ##### about to delete dataFile=[" + directory + "] and all of its content...");
    File[] files = new File(directory).listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        deleteDirectory(file.getAbsolutePath());
      } else {
        boolean successful = file.delete();
        if (!successful) { throw new AssertionError("delete file=[" + file.getAbsolutePath() + "] failed"); }
        debugPrintln("\n ##### deleted file=[" + file.getAbsolutePath() + "]");
      }
    }
    if (!(new File(directory).delete())) { throw new AssertionError("delete file=[" + directory + "] failed"); }
    debugPrintln("\n ##### deleted directory=[" + directory + "]");
    debugPrintln("\n ##### dataFile=[" + directory + "] still exists? [" + (new File(directory).exists()) + "]");
  }

  @Override
  public final String getConfigFileLocation() {
    return configFileLocation;
  }

  /*
   * Server inner class
   */
  private static class ServerInfo {

    private final int           server_dsoPort;
    private final ServerControl serverControl;

    ServerInfo(int dsoPort, ServerControl serverControl) {
      this.server_dsoPort = dsoPort;
      this.serverControl = serverControl;
    }

    public int getDsoPort() {
      return server_dsoPort;
    }

    public ServerControl getServerControl() {
      return serverControl;
    }
  }

  @Override
  public void crashActiveServers() throws Exception {
    crashActive();
  }

  public int[] getProxyDsoPorts() {
    return proxyDsoPorts;
  }

  public ProxyConnectManager[] getL1ProxyManagers() {
    return proxyL1Managers;
  }

  public ServerControl[] getServerControls() {
    ServerControl[] serverControls = new ServerControl[serverCount];
    for (int i = 0; i < serverCount; i++) {
      serverControls[i] = servers[i].getServerControl();
    }
    return serverControls;
  }

  public List<DSOMBean> connectAllDsoMBeans() throws IOException {
    List<DSOMBean> mbeans = new ArrayList<DSOMBean>();
    for (int i = 0; i < getServerCount(); i++) {
      mbeans.add(getDsoMBean(i));
    }
    return mbeans;
  }

  public List<DGCMBean> connectAllLocalDGCMBeans() throws IOException {
    List<DGCMBean> mbeans = new ArrayList<DGCMBean>();
    for (int i = 0; i < getServerCount(); i++) {
      mbeans.add(getLocalDGCMBean(i));
    }
    return mbeans;
  }

}

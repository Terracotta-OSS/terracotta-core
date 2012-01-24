package com.tc.test.setup;

import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.stats.api.DGCMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.proxy.ProxyConnectManager;
import com.tc.test.proxy.ProxyConnectManagerImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class GroupServerManager {

  private static final boolean      DEBUG            = Boolean.getBoolean("test.framework.debug");
  private final GroupsData          groupData;
  private final ServerControl[]     serverControl;
  private final TestConfig          testConfig;
  private static final String       HOST             = "localhost";
  private final File                tcConfigFile;
  private final File                javaHome;
  private final File                tempDir;
  private static final int          NULL_VAL         = -1;
  private final TCServerInfoMBean[] tcServerInfoMBeans;
  private final JMXConnector[]      jmxConnectors;
  private int                       lastCrashedIndex = NULL_VAL;
  private Random                    random;
  private long                      seed;

  protected ProxyConnectManager[]   proxyL2Managers;
  protected ProxyConnectManager[]   proxyL1Managers;
  private int                       activeIndex      = NULL_VAL;
  private final boolean[]           expectedServerRunning;
  private GroupServerCrashManager   serverCrasher;

  private ExecutorService           asyncExecutor    = Executors.newCachedThreadPool(new ThreadFactory() {
                                                       public Thread newThread(Runnable r) {
                                                         Thread t = new Thread(r, "Async Executor");
                                                         t.setDaemon(true);
                                                         return t;
                                                       }
                                                     });

  public GroupServerManager(GroupsData groupData, TestConfig testConfig, File tempDir, File javaHome, File tcConfigFile)
      throws Exception {
    this.groupData = groupData;
    this.serverControl = new ServerControl[groupData.getServerCount()];
    this.javaHome = javaHome;
    this.testConfig = testConfig;
    this.tempDir = tempDir;
    this.tcConfigFile = tcConfigFile;
    this.expectedServerRunning = new boolean[groupData.getServerCount()];
    createServers();
    this.tcServerInfoMBeans = new TCServerInfoMBean[groupData.getServerCount()];
    this.jmxConnectors = new JMXConnector[groupData.getServerCount()];
    SecureRandom srandom = SecureRandom.getInstance("SHA1PRNG");
    seed = srandom.nextLong();
    random = new Random(seed);
    // setup proxy
    if (isProxyL2GroupPort()) {
      proxyL2Managers = new ProxyConnectManager[groupData.getServerCount()];
      System.out.println("trying to setUp Proxy");
      for (int i = 0; i < groupData.getServerCount(); ++i) {
        proxyL2Managers[i] = new ProxyConnectManagerImpl(groupData.getL2GroupPort(i), groupData.getProxyL2GroupPort(i));
        proxyL2Managers[i].setupProxy();
      }
    }
    System.out.println("********");

    if (isProxyDsoPort()) {
      proxyL1Managers = new ProxyConnectManager[groupData.getServerCount()];
      for (int i = 0; i < groupData.getServerCount(); ++i) {
        proxyL1Managers[i] = new ProxyConnectManagerImpl(groupData.getDsoPort(i), groupData.getProxyDsoPort(i));
        proxyL1Managers[i].setupProxy();
      }
    }
    asyncExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Async Executor");
        t.setDaemon(true);
        return t;
      }
    });
    serverCrasher = new GroupServerCrashManager(testConfig, this);
  }

  private void createServers() {

    for (int i = 0; i < groupData.getServerCount(); i++) {
      ArrayList<String> perServerJvmArgs;
      if (isProxyL2GroupPort()) {
        perServerJvmArgs = testConfig.getL2Config().getExtraServerJvmArgs();
        // hidden tc.properties only used by L2 proxy testing purpose
        perServerJvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT + "="
                             + groupData.getL2GroupPort(i));
      } else {
        perServerJvmArgs = testConfig.getL2Config().getExtraServerJvmArgs();
      }

      TestBaseUtil.removeDuplicateJvmArgs(perServerJvmArgs);
      serverControl[i] = getServerControl(groupData.getDsoPort(i), groupData.getJmxPort(i),
                                          groupData.getServerNames()[i], perServerJvmArgs);
      expectedServerRunning[i] = false;
    }
  }

  private ServerControl getServerControl(int dsoPort, int jmxPort, String serverName, List aJvmArgs) {
    File workingDir = new File(this.tempDir, serverName);
    workingDir.mkdirs();
    return new ExtraProcessServerControl(HOST, dsoPort, jmxPort, tcConfigFile.getAbsolutePath(), true, serverName,
                                         aJvmArgs, javaHome, true, workingDir);
  }

  public void startAllServers() throws Exception {
    if (activeIndex >= 0) { throw new AssertionError("Server(s) has/have been already started"); }
    debugPrintln("***** startAllServers():  about to start [" + serverControl.length + "]servers  threadId=["
                 + Thread.currentThread().getName() + "]");
    for (int i = 0; i < serverControl.length; i++) {
      startServer(i);
    }
    Thread.sleep(500 * serverControl.length);

    debugPrintln("***** startAllServers():  about to search for active  threadId=[" + Thread.currentThread().getName()
                 + "]");
  }

  public synchronized void startServer(int index) throws Exception {
    verifyIndex(index);
    System.out.println("*** Starting server [" + serverControl[index].getDsoPort() + "] ... ");
    serverControl[index].start();
    if (isProxyL2GroupPort()) {
      proxyL2Managers[index].proxyUp();
      proxyL2Managers[index].startProxyTest();
      debugPrintln("***** Caching tcServerInfoMBean for server=[" + serverControl[index].getDsoPort() + "]");
      tcServerInfoMBeans[index] = getTcServerInfoMBean(index);
    }
    expectedServerRunning[index] = true;
    if (activeIndex < 0) {
      updateActiveIndex();
      startL1Proxy(activeIndex);
    }
    System.out.println("*** Server started [" + serverControl[index].getDsoPort() + "]");
  }

  private void verifyIndex(int serverIndex) {
    Assert.assertTrue("serverIndex " + serverIndex + " no. of servers per Group: " + groupData.getServerCount(),
                      serverIndex < groupData.getServerCount() && serverIndex >= 0);
  }

  private int expectedRunningServerCount() {
    int running = 0;
    for (boolean element : expectedServerRunning) {
      if (element) {
        running++;
      }
    }
    return running;
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  private synchronized void updateActiveIndex() throws Exception {
    int index = -1;
    resetActiveIndex();
    while (index < 0) {
      if (expectedRunningServerCount() == 0) {
        // No server is running
        return;
      }
      System.out.println("Searching for active server... ");
      for (int i = 0; i < groupData.getServerCount(); i++) {
        if (!expectedServerRunning[i]) {
          debugPrintln("Server[" + serverControl[i].getDsoPort()
                       + "] is not expected to be running. Skip checking its state");
          continue;
        }
        if (!serverControl[i].isRunning()) { throw new AssertionError("Server[" + serverControl[i].getDsoPort()
                                                                      + "] is not running as expected!"); }
        boolean isActive;
        try {
          isActive = tcServerInfoMBeans[i].isActive();
        } catch (Exception e) {
          System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[i].getDsoPort() + "]...");
          try {
            tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
            isActive = tcServerInfoMBeans[i].isActive();
          } catch (Exception e2) {
            System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
            continue;
          }
        }
        debugPrintln("********  index=[" + index + "]  i=[" + i + "] active=[" + isActive + "] lastCrashedIndex=["
                     + lastCrashedIndex + "] threadId=[" + Thread.currentThread().getName() + "]");

        if (isActive) {
          if (index < 0) {
            index = i;
            debugPrintln("***** active found index=[" + index + "]");
          } else {
            throw new Exception("More than one active server found.");
          }
        }
      }
      Thread.sleep(1000);
    }
    activeIndex = index;
  }

  private void startL1Proxy(int index) {
    if (isProxyDsoPort()) {
      System.out.println("*** Starting the DSO proxy with proxy port as " + proxyL1Managers[index].getProxyPort()
                         + " and DSO port as " + proxyL1Managers[index].getDsoPort());
      proxyL1Managers[index].proxyUp();
      proxyL1Managers[index].startProxyTest();
    }
  }

  public void stopAllServers() throws Exception {
    closeJMXConnectors();

    for (int i = 0; i < serverControl.length; i++) {
      if (serverControl[i].isRunning()) {
        stopServerInternal(i);
      }
    }
  }

  public void stopServer(int index) throws Exception {
    stopServerInternal(index);
    if (index == activeIndex) {
      updateActiveIndex();
    }
  }

  private synchronized void stopServerInternal(int index) throws Exception {

    if (!expectedServerRunning[index]) {
      System.out.println("***Server not expected to be running. not stopping server ["
                         + serverControl[index].getDsoPort() + "]");
      return;
    }
    System.out.println("*** stopping server [" + serverControl[index].getDsoPort() + "]");
    ServerControl sc = serverControl[index];

    if (!sc.isRunning()) { throw new AssertionError(
                                                    "Server["
                                                        + serverControl[index].getDsoPort()
                                                        + "] is not running as expected. State Found:[STOPPED] Expected:[RUNNING]!"); }

    if (index == activeIndex) {
      sc.shutdown();
      stopL2GroupProxy(index);
      stopL1Proxy(index);
      System.out.println("*** Server(active) stopped [" + serverControl[index].getDsoPort() + "]");
      return;
    }

    try {
      sc.crash();
      stopL2GroupProxy(index);
    } catch (Exception e) {
      if (DEBUG) {
        e.printStackTrace();
      }
    }
    System.out.println("*** Server stopped [" + serverControl[index].getDsoPort() + "]");
  }

  private void stopL2GroupProxy(int index) {
    if (isProxyL2GroupPort()) {
      proxyL2Managers[index].proxyDown();
    }
  }

  private void stopL1Proxy(int index) {
    if (isProxyDsoPort()) {
      proxyL1Managers[index].proxyDown();
    }
  }

  private void cleanupServerDB(int index) throws Exception {
    if (testConfig.getL2Config().getPersistenceMode().equals(PersistenceMode.PERMANENT_STORE)) {
      System.out.println("Deleting data directory for server=[" + serverControl[index].getDsoPort() + "]");
      deleteDirectory(groupData.getDataDirectoryPath(index));
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

  private TCServerInfoMBean getTcServerInfoMBean(int index) throws IOException {
    if (jmxConnectors[index] != null) {
      closeJMXConnector(index);
    }
    jmxConnectors[index] = getJMXConnector(serverControl[index].getAdminPort());
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                         TCServerInfoMBean.class, true);
  }

  public static JMXConnector getJMXConnector(int jmxPort) throws IOException {
    return JMXUtils.getJMXConnector("localhost", jmxPort);
  }

  public DSOMBean getDsoMBean(int index) throws IOException {
    JMXConnector jmxc = getJMXConnector(serverControl[index].getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DSOMBean dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    return dsoMBean;
  }

  public DGCMBean getLocalDGCMBean(int index) throws IOException {
    JMXConnector jmxc = getJMXConnector(serverControl[index].getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DGCMBean dgcMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.LOCAL_DGC_STATS,
                                                                      DGCMBean.class, false);
    return dgcMBean;
  }

  private void closeJMXConnector(int i) {
    if (jmxConnectors[i] != null) {
      try {
        jmxConnectors[i].close();
      } catch (Exception e) {
        System.out.println("JMXConnector for server=[" + serverControl[i].getDsoPort() + "] already closed.");
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

  public List<DSOMBean> connectAllDsoMBeans() throws IOException {
    List<DSOMBean> mbeans = new ArrayList<DSOMBean>();
    for (int i = 0; i < serverControl.length; i++) {
      mbeans.add(getDsoMBean(i));
    }
    return mbeans;
  }

  public List<DGCMBean> connectAllLocalDGCMBeans() throws IOException {
    List<DGCMBean> mbeans = new ArrayList<DGCMBean>();
    for (int i = 0; i < serverControl.length; i++) {
      mbeans.add(getLocalDGCMBean(i));
    }
    return mbeans;
  }

  public ServerControl[] getServerControls() {
    return serverControl;
  }

  public GroupsData getGroupData() {
    return groupData;
  }

  public synchronized void crashActiveAndWaitForPassiveToTakeOver() throws Exception {
    crashActiveInternal();

    if (expectedRunningServerCount() > 0) {
      debugPrintln("***** about to search for active  threadId=[" + Thread.currentThread().getName() + "]");
      updateActiveIndex();
      startL1Proxy(activeIndex);
    }

    debugPrintln("***** activeIndex[" + activeIndex + "] ");
    System.out.println("******* Done Crashing active server");

  }

  public synchronized void crashActive() throws Exception {
    crashActiveInternal();
    if (expectedRunningServerCount() > 0) {
      asyncExecutor.execute(new Runnable() {

        @Override
        public void run() {
          debugPrintln("***** about to search for active  threadId=[" + Thread.currentThread().getName() + "]");
          try {
            updateActiveIndex();
          } catch (Exception e) {
            e.printStackTrace();
          }
          startL1Proxy(activeIndex);

        }
      });
    }

  }

  private void crashActiveInternal() throws Exception {
    System.out.println("******** Crashing active Server");

    if (activeIndex < 0) { throw new AssertionError("Active index was not set.No Active Server For Crashing"); }

    System.out.println("Crashing active server: dsoPort=[" + serverControl[activeIndex].getDsoPort() + "]");
    if (expectedRunningServerCount() > 1) {
      waituntilPassiveStandBy();
    }

    verifyActiveServerState();

    ServerControl server = serverControl[activeIndex];
    server.crash();
    debugPrintln("***** Sleeping after crashing active server ");
    waitForServerCrash(server);
    stopL2GroupProxy(activeIndex);
    stopL1Proxy(activeIndex);
    expectedServerRunning[activeIndex] = false;
    debugPrintln("***** Done sleeping after crashing active server ");
    lastCrashedIndex = activeIndex;
    resetActiveIndex();
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");
    if (expectedRunningServerCount() > 0) {
      cleanupServerDB(lastCrashedIndex);
    }
  }

  public void crashAllPassive() throws Exception {
    System.out.println("**** Crashing all passives");
    for (int i = 0; i < groupData.getServerCount(); i++) {
      if (i != activeIndex && expectedServerRunning[i]) {
        crashPassive(i);
      }
    }
    System.out.println("***** Done Crashing all passives");

  }

  public synchronized void crashPassive(int passiveToCrash) throws Exception {
    verifyIndex(passiveToCrash);
    if (activeIndex == passiveToCrash) { throw new AssertionError("Crash Passive Cannot crash active server["
                                                                  + serverControl[passiveToCrash].getDsoPort() + "]"); }

    System.out.println("Crashing passive server: dsoPort=[" + serverControl[passiveToCrash].getDsoPort() + "]");

    debugPrintln("***** Closing passive's jmxConnector ");
    closeJMXConnector(passiveToCrash);

    ServerControl server = serverControl[passiveToCrash];
    if (!server.isRunning()) { throw new AssertionError("Server[" + server.getDsoPort()
                                                        + "] is not running as expected!"); }
    server.crash();
    debugPrintln("***** Sleeping after crashing passive server ");
    waitForServerCrash(server);
    stopL2GroupProxy(passiveToCrash);
    expectedServerRunning[passiveToCrash] = false;
    debugPrintln("***** Done sleeping after crashing passive server ");

    lastCrashedIndex = passiveToCrash;
    if (groupData.getServerCount() > 1) {
      cleanupServerDB(lastCrashedIndex);
    }
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");
  }

  public synchronized void crashRandomServer() throws Exception {

    if (activeIndex < 0) { throw new AssertionError("Active index was not set."); }
    if (random == null) { throw new AssertionError("Random number generator was not set."); }

    debugPrintln("***** Choosing random server... ");

    int crashIndex = random.nextInt(groupData.getServerCount());
    // TODO If crashIndex selected has not been started yet ??
    crashServer(crashIndex);
  }

  public synchronized void crashServer(int crashIndex) throws Exception {
    verifyIndex(crashIndex);
    if (crashIndex == activeIndex) {
      crashActive();
    } else {
      crashPassive(crashIndex);
    }
  }

  public void restartLastCrashedServer() throws Exception {

    debugPrintln("*****  restarting last crashed server");

    if (lastCrashedIndex >= 0) {
      if (serverControl[lastCrashedIndex].isRunning()) { throw new AssertionError(
                                                                                  "Server["
                                                                                      + serverControl[lastCrashedIndex]
                                                                                          .getDsoPort()
                                                                                      + "] is not down as expected!"); }
      restartCrashedServer(lastCrashedIndex);
    } else {
      throw new AssertionError("No crashed servers to restart.");
    }
  }

  public void restartCrashedServer(int serverIndex) throws Exception {

    debugPrintln("*****  restarting crashed server");

    if (serverControl[serverIndex].isRunning()) { throw new AssertionError("Server["
                                                                           + serverControl[serverIndex].getDsoPort()
                                                                           + "] is not down as expected!"); }
    startServer(serverIndex);
    if (serverIndex == lastCrashedIndex) {
      resetLastCrashedIndex();
    }
  }

  private void resetActiveIndex() {
    activeIndex = NULL_VAL;
  }

  private void resetLastCrashedIndex() {
    lastCrashedIndex = NULL_VAL;
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

  private void verifyActiveServerState() throws Exception {
    ServerControl server = serverControl[activeIndex];
    if (!server.isRunning()) { throw new AssertionError("Server[" + serverControl[activeIndex].getDsoPort()
                                                        + "] is not running as expected!"); }

    if (jmxConnectors[activeIndex] == null) {
      jmxConnectors[activeIndex] = getJMXConnector(serverControl[activeIndex].getAdminPort());
    }
    MBeanServerConnection mbs = jmxConnectors[activeIndex].getMBeanServerConnection();
    TCServerInfoMBean mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.TC_SERVER_INFO,
                                                                            TCServerInfoMBean.class, true);
    if (!mbean.isActive()) {
      closeJMXConnector(activeIndex);
      throw new AssertionError("Server[" + serverControl[activeIndex].getDsoPort()
                               + "] is not an active server as expected!");
    }
    closeJMXConnector(activeIndex);
  }

  public void waituntilPassiveStandBy() throws Exception {
    while (!isPassiveStandBy()) {
      Thread.sleep(1000);
    }
  }

  private boolean isProxyL2GroupPort() {
    return testConfig.getL2Config().isProxyL2groupPorts();
  }

  private boolean isProxyDsoPort() {
    return testConfig.getL2Config().isProxyDsoPorts();
  }

  public boolean dumpClusterState(int dumpCount, long dumpInterval) throws Exception {

    boolean dumpTaken = false;
    if (activeIndex < 0) {
      updateActiveIndex();
    }

    if (serverControl[activeIndex].isRunning()) {
      System.out.println("Dumping server=[" + serverControl[activeIndex].getDsoPort() + "]");

      MBeanServerConnection mbs;
      try {
        if (jmxConnectors[activeIndex] == null) {
          jmxConnectors[activeIndex] = getJMXConnector(serverControl[activeIndex].getAdminPort());
        }
        mbs = jmxConnectors[activeIndex].getMBeanServerConnection();
      } catch (IOException ioe) {
        System.out.println("Need to recreate jmxConnector for server=[" + serverControl[activeIndex].getDsoPort()
                           + "]...");
        jmxConnectors[activeIndex] = getJMXConnector(serverControl[activeIndex].getAdminPort());
        mbs = jmxConnectors[activeIndex].getMBeanServerConnection();
      }

      L2DumperMBean mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                          L2DumperMBean.class, true);
      try {
        mbean.dumpClusterState();
        return true;
      } catch (Exception e) {
        System.out.println("Could not find L2DumperMBean... sleep for 1 sec.");
        Thread.sleep(1000);
      }

      mbean.setThreadDumpCount(dumpCount);
      mbean.setThreadDumpInterval(dumpInterval);
      System.out.println("Thread dumping server=[" + serverControl[activeIndex].getDsoPort() + "] ");
      mbean.doThreadDump();

    }

    closeJMXConnectors();
    return dumpTaken;

  }

  public synchronized boolean isActivePresent() {
    return activeIndex < 0 ? false : true;
  }

  public boolean isPassiveStandBy() {

    System.out.println("Searching for appropriate passive server(s)... ");
    for (int i = 0; i < groupData.getServerCount(); i++) {
      try {
        if (tcServerInfoMBeans[i].isPassiveStandby()) { return true; }
      } catch (Exception e) {
        System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[i].getDsoPort() + "]... ["
                           + e.getMessage() + "]");
        try {
          tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
          if (tcServerInfoMBeans[i].isPassiveStandby()) return true;
        } catch (Exception e2) {
          System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
        }
      }
    }

    return false;
  }

  public void startCrasher() {
    if (!testConfig.getCrashConfig().getCrashMode().equals(ServerCrashMode.NO_CRASH)
        && !testConfig.getCrashConfig().getCrashMode().equals(ServerCrashMode.CUSTOMIZED_CRASH)) {
      new Thread(serverCrasher).start();
    }
  }

  public void stopCrasher() {
    this.serverCrasher.stop();
  }
}

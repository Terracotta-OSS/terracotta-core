package com.tc.test.setup;

import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.TestFailureListener;

import com.tc.lang.ServerExitStatus;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.stats.api.DGCMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.config.model.L2Config;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.proxy.ProxyConnectManager;
import com.tc.test.proxy.ProxyConnectManagerImpl;
import com.tc.text.Banner;
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

  private static final String       DEBUG_SERVER_PROPERTY = "l2.debug";
  private static final boolean      DEBUG                 = Boolean.getBoolean("test.framework.debug");
  /**
   * If set to true allows remote debugging of java applications. Do Not Use This Flag with Crashing Enabled.
   */
  private static final boolean      DEBUG_SERVER          = Boolean.getBoolean(DEBUG_SERVER_PROPERTY);
  private final GroupsData          groupData;
  private final ServerControl[]     serverControl;
  private final TestConfig          testConfig;
  private static final String       HOST                  = "localhost";
  private final File                tcConfigFile;
  private final File                javaHome;
  private final File                tempDir;
  private static final int          NULL_VAL              = -1;
  private final TCServerInfoMBean[] tcServerInfoMBeans;
  private final JMXConnector[]      jmxConnectors;
  private int                       lastCrashedIndex      = NULL_VAL;
  private Random                    random;
  private long                      seed;

  protected ProxyConnectManager[]   proxyL2Managers;
  protected ProxyConnectManager[]   proxyL1Managers;
  private final boolean[]           expectedServerRunning;
  private GroupServerCrashManager   serverCrasher;

  private ExecutorService           asyncExecutor         = Executors.newCachedThreadPool(new ThreadFactory() {
                                                            @Override
                                                            public Thread newThread(Runnable r) {
                                                              Thread t = new Thread(r, "Async Executor");
                                                              t.setDaemon(true);
                                                              return t;
                                                            }
                                                          });

  private final TestFailureListener testFailureCallback;
  private final boolean             renameDataDir         = false;

  private final class ServerExitCallback implements MonitoringServerControl.MonitoringServerControlExitCallback {

    private final String serverName;
    private final int    dsoPort;

    private ServerExitCallback(String server, int port) {
      serverName = server;
      dsoPort = port;
    }

    @Override
    public boolean onExit(final int exitCode) {

      String errMsg;
      if (exitCode == ServerExitStatus.EXITCODE_RESTART_REQUEST && testConfig.isRestartZappedL2()) {
        errMsg = "*** Server '" + serverName + "' with dso-port " + dsoPort
                 + " was zapped and needs to be restarted! ***";
        System.out.println(errMsg);
        return true;
      }
      errMsg = "*** Server '" + serverName + "' with dso-port " + dsoPort + " exited unexpectedly with exit code "
               + exitCode + ". ***";
      System.err.println(errMsg);
      if (!testConfig.getCrashConfig().shouldIgnoreUnexpectedL2Crash()) {
        testFailureCallback.testFailed(errMsg);
      }
      return false;
    }

  }

  public GroupServerManager(GroupsData groupData, TestConfig testConfig, File tempDir, File javaHome,
                            File tcConfigFile, final TestFailureListener testFailureCallback) throws Exception {
    this.groupData = groupData;
    this.testFailureCallback = testFailureCallback;
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
        proxyL2Managers[i].setProxyWaitTime(this.testConfig.getL2Config().getProxyWaitTime());
        proxyL2Managers[i].setProxyDownTime(this.testConfig.getL2Config().getProxyDownTime());
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
      @Override
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
      ArrayList<String> perServerJvmArgs = new ArrayList<String>();
      L2Config l2Config = testConfig.getL2Config(groupData.getGroupIndex(), i);
      perServerJvmArgs.addAll(l2Config.getExtraServerJvmArgs());
      if (isProxyL2GroupPort()) {
        // hidden tc.properties only used by L2 proxy testing purpose
        perServerJvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT + "="
                             + groupData.getL2GroupPort(i));
      }
      if (shouldDebugServer(i)) {
        int debugPort = 11000 + i;
        perServerJvmArgs.add("-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=" + debugPort);
        Banner.infoBanner("waiting for debugger to attach on port " + debugPort);
      }
      serverControl[i] = getServerControl(groupData.getDsoPort(i), groupData.getJmxPort(i),
                                          groupData.getServerNames()[i], perServerJvmArgs, l2Config);
      expectedServerRunning[i] = false;
    }
  }

  private boolean shouldDebugServer(int debugPortOffset) {
    return DEBUG_SERVER || Boolean.getBoolean(DEBUG_SERVER_PROPERTY + "." + debugPortOffset);
  }

  private ServerControl getServerControl(final int dsoPort, final int jmxPort, final String serverName,
                                         List<String> jvmArgs, final L2Config l2config) {
    File workingDir = new File(this.tempDir, serverName);
    workingDir.mkdirs();
    File verboseGcOutputFile = new File(workingDir, "verboseGC.log");
    TestBaseUtil.setupVerboseGC(jvmArgs, verboseGcOutputFile);
    TestBaseUtil.setHeapSizeArgs(jvmArgs, l2config.getMinHeap(), l2config.getMaxHeap(), l2config.getDirectMemorySize());
    TestBaseUtil.removeDuplicateJvmArgs(jvmArgs);
    l2config.getBytemanConfig().addTo(jvmArgs, tempDir);
    return new MonitoringServerControl(new ExtraProcessServerControl(HOST, dsoPort, jmxPort,
                                                                     tcConfigFile.getAbsolutePath(), true, serverName,
                                                                     jvmArgs, javaHome, true, workingDir),
                                       new ServerExitCallback(serverName, dsoPort));
  }

  public void startAllServers() throws Exception {
    debugPrintln("***** startAllServers():  about to start [" + serverControl.length + "]servers  threadId=["
                 + Thread.currentThread().getName() + "]");
    for (int i = 0; i < serverControl.length; i++) {
      if (expectedServerRunning[i]) {
        debugPrintln("***** startAllServers():  Not starting already running server [" + serverControl[i].getDsoPort()
                     + "]  threadId=[" + Thread.currentThread().getName() + "]");
        continue;
      }
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
    // this is the only server running. start an async thread to start l1 proxy when the server becomes active.
    if (expectedRunningServerCount() == 1) {
      startL1ProxyOnActiveServer();
    }
    System.out.println("*** Server started [" + serverControl[index].getDsoPort() + "]");
  }

  public synchronized void startServerNoWait(int index) throws Exception {
    verifyIndex(index);
    System.out.println("*** Starting server [" + serverControl[index].getDsoPort() + "] expecting a crash. ");
    serverControl[index].startWithoutWait();
    expectedServerRunning[index] = false;
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

  private void startL1ProxyOnActiveServer() {
    if (isProxyDsoPort()) {
      asyncExecutor.submit(new Runnable() {
        @Override
        public void run() {

          int activeServer = getActiveServerIndex();
          while (activeServer < 0) {
            ThreadUtil.reallySleep(1000);
            activeServer = getActiveServerIndex();
          }
          startL1Proxy(activeServer);
        }
      });
    }
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
  }

  private synchronized void stopServerInternal(int index) throws Exception {

    if (!expectedServerRunning[index]) {
      System.out.println("***Server not expected to be running. not stopping server ["
                         + serverControl[index].getDsoPort() + "]");
      return;
    }
    boolean active = isActive(index);
    System.out.println("*** stopping server [" + serverControl[index].getDsoPort() + "]");
    ServerControl sc = serverControl[index];

    if (!sc.isRunning()) { throw new AssertionError(
                                                    "Server["
                                                        + serverControl[index].getDsoPort()
                                                        + "] is not running as expected. State Found:[STOPPED] Expected:[RUNNING]!"); }
    sc.shutdown();
    stopL2GroupProxy(index);
    expectedServerRunning[index] = false;
    if (active) {
      stopL1Proxy(index);
      if (expectedRunningServerCount() > 0) {
        startL1ProxyOnActiveServer();
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
    if (testConfig.getL2Config().getRestartable()) {
      if (renameDataDir) {
        System.out.println("Moving data directory for server=[" + serverControl[index].getDsoPort() + "]");
        renameDir(groupData.getDataDirectoryPath(index));
      } else {
        System.out.println("Deleting data directory for server=[" + serverControl[index].getDsoPort() + "]");
        deleteDirectory(groupData.getDataDirectoryPath(index));
      }
    }
  }

  private void renameDir(String path) {
    Assert.assertTrue(new File(path).renameTo(new File(path + "-" + System.currentTimeMillis())));
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

  public List<DSOMBean> connectAllDsoMBeans() {
    List<DSOMBean> mbeans = new ArrayList<DSOMBean>();
    for (int i = 0; i < serverControl.length; i++) {
      try {
        mbeans.add(getDsoMBean(i));
      } catch (IOException e) {
        System.out.println("XXXXXXX could not connect to server[" + serverControl[i].getDsoPort() + "], jmxPort:"
                           + serverControl[i].getAdminPort());
      }
    }
    return mbeans;
  }

  public List<DGCMBean> connectAllLocalDGCMBeans() {
    List<DGCMBean> mbeans = new ArrayList<DGCMBean>();
    for (int i = 0; i < serverControl.length; i++) {
      try {
        mbeans.add(getLocalDGCMBean(i));
      } catch (IOException e) {
        System.out.println("XXXXXXX could not connect to server[" + serverControl[i].getDsoPort() + "], jmxPort:"
                           + serverControl[i].getAdminPort());
      }
    }
    return mbeans;
  }

  public GroupsData getGroupData() {
    return groupData;
  }

  public synchronized void crashActiveAndWaitForPassiveToTakeOver() throws Exception {
    crashActive();
    int activeServer = getActiveServerIndex();
    while (activeServer < 0) {
      ThreadUtil.reallySleep(1000);
      activeServer = getActiveServerIndex();
    }
    System.out.println("******* Done Crashing active server");

  }

  public synchronized void crashActive() throws Exception {
    int activeIndex = getActiveServerIndex();
    if (activeIndex < 0) { throw new AssertionError("Trying to crash active server when no active server is present"); }
    crashServer(activeIndex);
    System.out.println("******* Done Crashing active server");
  }

  private synchronized int getActiveServerIndex() {
    System.out.println("Searching for active server... ");
    for (int index = 0; index < groupData.getServerCount(); index++) {
      if (isActive(index)) { return index; }
    }
    return -1;

  }

  public void crashAllPassive() throws Exception {
    System.out.println("**** Crashing all passives");
    int activeIndex = getActiveServerIndex();
    for (int i = 0; i < groupData.getServerCount(); i++) {
      if (i != activeIndex && expectedServerRunning[i]) {
        crashServer(i);
      }
    }
    System.out.println("***** Done Crashing all passives");

  }

  public synchronized void crashPassive(int passiveToCrash) throws Exception {
    verifyIndex(passiveToCrash);
    if (isActive(passiveToCrash)) { throw new AssertionError("**** Trying to crash server ["
                                                             + serverControl[passiveToCrash].getDsoPort()
                                                             + "] as passive server but it is in ACTIVE state."); }
    crashServer(passiveToCrash);
  }

  public synchronized void crashRandomServer() throws Exception {

    if (random == null) { throw new AssertionError("Random number generator was not set."); }

    debugPrintln("***** Choosing random server... ");

    int crashIndex = random.nextInt(groupData.getServerCount());
    if (expectedServerRunning[crashIndex]) {
      crashServer(crashIndex);
    }
  }

  public synchronized void crashServer(int index) throws Exception {
    System.out.println("******** Crashing active Server");

    boolean active = isActive(index);
    System.out.println("Crashing active server: dsoPort=[" + serverControl[index].getDsoPort() + "]");
    if (expectedRunningServerCount() > 1) {
      waituntilPassiveStandBy();
    }
    ServerControl server = serverControl[index];
    server.crash();
    debugPrintln("***** Sleeping after crashing active server ");
    waitForServerCrash(server);
    stopL2GroupProxy(index);
    expectedServerRunning[index] = false;
    // If active server is crashed. stop l1 proxy and start it on the new active in async thread.
    if (active) {
      stopL1Proxy(index);
      if (expectedRunningServerCount() > 1) {
        startL1ProxyOnActiveServer();
      }
    }
    debugPrintln("***** Done sleeping after crashing active server ");
    lastCrashedIndex = index;
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");
    if (expectedRunningServerCount() > 0 && testConfig.getCrashConfig().shouldCleanDbOnCrash()) {
      cleanupServerDB(lastCrashedIndex);
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

  public boolean isServerRunning(int index) {
    return serverControl[index] != null && serverControl[index].isRunning();
  }

  public void waituntilPassiveStandBy() throws Exception {
    while (!isPassiveStandBy()) {
      Thread.sleep(1000);
    }
  }

  public void waituntilEveryPassiveStandBy() throws Exception {
    while (!isEveryPassiveStandBy()) {
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
    int serverIndex = getActiveServerIndex();
    if (serverIndex != -1) {
      dumpTaken = dumpClusterStateInternal(dumpCount, dumpInterval, serverIndex);
    } else {
      // active server not present dump all passives.
      for (int i = 0; i < groupData.getServerCount(); i++) {
        dumpTaken = dumpTaken | dumpClusterStateInternal(dumpCount, dumpInterval, i);
      }

    }
    return dumpTaken;

  }

  private boolean dumpClusterStateInternal(int dumpCount, long dumpInterval, int serverIndex) throws IOException,
      InterruptedException, Exception {
    if (serverControl[serverIndex].isRunning()) {
      System.out.println("Dumping server=[" + serverControl[serverIndex].getDsoPort() + "]");

      MBeanServerConnection mbs;
      try {
        if (jmxConnectors[serverIndex] == null) {
          jmxConnectors[serverIndex] = getJMXConnector(serverControl[serverIndex].getAdminPort());
        }
        mbs = jmxConnectors[serverIndex].getMBeanServerConnection();
      } catch (IOException ioe) {
        System.out.println("Need to recreate jmxConnector for server=[" + serverControl[serverIndex].getDsoPort()
                           + "]...");
        jmxConnectors[serverIndex] = getJMXConnector(serverControl[serverIndex].getAdminPort());
        mbs = jmxConnectors[serverIndex].getMBeanServerConnection();
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
      System.out.println("Thread dumping server=[" + serverControl[serverIndex].getDsoPort() + "] ");
      mbean.doThreadDump();

    }

    closeJMXConnectors();
    return false;
  }

  public synchronized boolean isActivePresent() {
    return getActiveServerIndex() == -1 ? false : true;
  }

  public boolean isEveryPassiveStandBy() {

    System.out.println("Searching for appropriate passive server(s)... ");
    int passives = 0;
    int expectedPassives = -1;
    for (int i = 0; i < groupData.getServerCount(); i++) {
      try {
        if (expectedServerRunning[i]) {
          expectedPassives++;
        }
        if (tcServerInfoMBeans[i].isPassiveStandby()) {
          passives++;
        }
      } catch (Exception e) {
        System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[i].getDsoPort() + "]... ["
                           + e.getMessage() + "]");
        try {
          tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
          if (tcServerInfoMBeans[i].isPassiveStandby()) passives++;
        } catch (Exception e2) {
          System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
        }
      }
    }

    return passives == expectedPassives;
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

  public boolean isActive(int index) {
    boolean isActive = false;
    try {
      isActive = tcServerInfoMBeans[index].isActive();
    } catch (Exception e) {
      System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[index].getDsoPort() + "]...");
      try {
        tcServerInfoMBeans[index] = getTcServerInfoMBean(index);
        isActive = tcServerInfoMBeans[index].isActive();
      } catch (Exception e2) {
        System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
      }
    }
    return isActive;
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

  public void stopDsoProxy() {
    for (ProxyConnectManager proxy : this.proxyL1Managers) {
      proxy.closeClientConnections();
    }
  }

  public int waitForServerExit(int serverIndex) throws Exception {
    return serverControl[serverIndex].waitFor();
  }
}

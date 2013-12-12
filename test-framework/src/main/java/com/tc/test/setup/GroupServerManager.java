package com.tc.test.setup;

import org.junit.Assert;
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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

class GroupServerManager {

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
  private final AtomicBoolean       crasherStarted        = new AtomicBoolean(false);

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
  private volatile boolean          stopped               = false;
  private Thread                    crasherThread;

  private final class ServerExitCallback implements MonitoringServerControl.MonitoringServerControlExitCallback {

    private final String serverName;
    private final int    dsoPort;
    private final int    index;

    private ServerExitCallback(String server, int port, int index) {
      serverName = server;
      dsoPort = port;
      this.index = index;
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
      expectedServerRunning[index] = false;

      errMsg = "*** Server '" + serverName + "' with dso-port " + dsoPort + " exited unexpectedly with exit code "
               + exitCode + ". ***";
      System.err.println(errMsg);
      if (!testConfig.getCrashConfig().shouldIgnoreUnexpectedL2Crash()) {
        testFailureCallback.testFailed(errMsg);
      }
      return false;
    }

  }

  GroupServerManager(GroupsData groupData, TestConfig testConfig, File tempDir, File javaHome, File tcConfigFile,
                     final TestFailureListener testFailureCallback) throws Exception {
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
    if (isProxyTsaGroupPort()) {
      proxyL2Managers = new ProxyConnectManager[groupData.getServerCount()];
      System.out.println("trying to setUp Proxy");
      for (int i = 0; i < groupData.getServerCount(); ++i) {
        proxyL2Managers[i] = new ProxyConnectManagerImpl(groupData.getTsaGroupPort(i),
                                                         groupData.getProxyTsaGroupPort(i));
        proxyL2Managers[i].setManualControl(this.testConfig.getL2Config().isManualProxycontrol());
        proxyL2Managers[i].setProxyWaitTime(this.testConfig.getL2Config().getProxyWaitTime());
        proxyL2Managers[i].setProxyDownTime(this.testConfig.getL2Config().getProxyDownTime());
        proxyL2Managers[i].setupProxy();

      }
    }
    System.out.println("********");

    if (isProxyTsaPort()) {
      proxyL1Managers = new ProxyConnectManager[groupData.getServerCount()];
      for (int i = 0; i < groupData.getServerCount(); ++i) {
        proxyL1Managers[i] = new ProxyConnectManagerImpl(groupData.getTsaPort(i), groupData.getProxyTsaPort(i));
        proxyL1Managers[i].setManualControl(this.testConfig.getL2Config().isManualProxycontrol());
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
      if (isProxyTsaGroupPort()) {
        // hidden tc.properties only used by L2 proxy testing purpose
        perServerJvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT + "="
                             + groupData.getTsaGroupPort(i));
      }
      if (shouldDebugServer(i)) {
        int debugPort = 11000 + i;
        perServerJvmArgs.add("-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=" + debugPort);
        Banner.infoBanner("waiting for debugger to attach on port " + debugPort);
      }
      serverControl[i] = getServerControl(groupData.getTsaPort(i), groupData.getJmxPort(i),
                                          groupData.getServerNames()[i], perServerJvmArgs, l2Config, i);
      expectedServerRunning[i] = false;
    }
  }

  private boolean shouldDebugServer(int debugPortOffset) {
    return DEBUG_SERVER || Boolean.getBoolean(DEBUG_SERVER_PROPERTY + "." + debugPortOffset);
  }

  private ServerControl getServerControl(final int dsoPort, final int jmxPort, final String serverName,
                                         List<String> jvmArgs, final L2Config l2config, final int index) {
    File workingDir = new File(this.tempDir, serverName);
    workingDir.mkdirs();
    File verboseGcOutputFile = new File(workingDir, "verboseGC.log");
    TestBaseUtil.setupVerboseGC(jvmArgs, verboseGcOutputFile);
    TestBaseUtil.setHeapSizeArgs(jvmArgs, l2config.getMinHeap(), l2config.getMaxHeap(), l2config.getDirectMemorySize(),
                                 false);
    TestBaseUtil.removeDuplicateJvmArgs(jvmArgs);
    l2config.getBytemanConfig().addTo(jvmArgs, tempDir);
    return new MonitoringServerControl(new ExtraProcessServerControl(HOST, dsoPort, jmxPort,
                                                                     tcConfigFile.getAbsolutePath(), true, serverName,
                                                                     jvmArgs, javaHome, true, workingDir),
                                       new ServerExitCallback(serverName, dsoPort, index));
  }

  void startAllServers() throws Exception {
    debugPrintln("***** startAllServers():  about to start [" + serverControl.length + "]servers  threadId=["
                 + Thread.currentThread().getName() + "]");
    for (int i = 0; i < serverControl.length; i++) {
      if (expectedServerRunning[i]) {
        debugPrintln("***** startAllServers():  Not starting already running server [" + serverControl[i].getTsaPort()
                     + "]  threadId=[" + Thread.currentThread().getName() + "]");
        continue;
      }
      startServer(i);
      waitUntilActive();
    }
    Thread.sleep(500 * serverControl.length);

    debugPrintln("***** startAllServers():  about to search for active  threadId=[" + Thread.currentThread().getName()
                 + "]");
  }

  synchronized void startServer(int index) throws Exception {
    verifyIndex(index);
    System.out.println("*** Starting server [" + serverControl[index].getTsaPort() + "] ... ");
    serverControl[index].start();
    if (isProxyTsaGroupPort()) {
      proxyL2Managers[index].proxyUp();
      proxyL2Managers[index].startProxyTest();
      debugPrintln("***** Caching tcServerInfoMBean for server=[" + serverControl[index].getTsaPort() + "]");
      tcServerInfoMBeans[index] = getTcServerInfoMBean(index);
    }
    expectedServerRunning[index] = true;
    // this is the only server running. start an async thread to start l1 proxy when the server becomes active.
    if (expectedRunningServerCount() == 1) {
      startL1ProxyOnActiveServerAsync();
    }
    System.out.println("*** Server started [" + serverControl[index].getTsaPort() + "]");
  }

  synchronized void startServerNoWait(int index) throws Exception {
    verifyIndex(index);
    System.out.println("*** Starting server [" + serverControl[index].getTsaPort() + "] expecting a crash. ");
    serverControl[index].startWithoutWait();
    expectedServerRunning[index] = false;
    System.out.println("*** Server started [" + serverControl[index].getTsaPort() + "]");
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

  private void startL1ProxyOnActiveServerAsync() {
    if (isProxyTsaPort()) {
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

  void startTsaProxyOnActiveServer() {
    if (isProxyTsaPort()) {
      int activeServerIndex = getActiveServerIndex();
      while (activeServerIndex < 0) {
        ThreadUtil.reallySleep(1000);
        activeServerIndex = getActiveServerIndex();
      }
      startL1Proxy(activeServerIndex);
    }
  }

  private void startL1Proxy(int index) {
    if (isProxyTsaPort()) {
      System.out.println("*** Starting the DSO proxy with proxy port as " + proxyL1Managers[index].getProxyPort()
                         + " and DSO port as " + proxyL1Managers[index].getTsaPort());
      proxyL1Managers[index].proxyUp();
      proxyL1Managers[index].startProxyTest();
    }
  }

  private void stopAllServers() throws Exception {
    closeJMXConnectors();
    for (int i = 0; i < serverControl.length; i++) {
      synchronized (this) {
        if (serverControl[i].isRunning()) {
          stopServer(i);
        }
      }
    }
  }

  void stop() throws Exception {

    stopCrasher();
    // XXX as crasher is too coupled with this class right now, we only want to set this flag after it finishes
    stopped = true;

    stopAllServers();
  }

  private void stopServer(int index) throws Exception {
    Assert.assertTrue(Thread.holdsLock(this));
    if (!expectedServerRunning[index]) {
      System.out.println("***Server not expected to be running. not stopping server ["
                         + serverControl[index].getTsaPort() + "]");
      return;
    }
    boolean active = isActive(index);
    System.out.println("*** stopping server [" + serverControl[index].getTsaPort() + "]");
    ServerControl sc = serverControl[index];

    if (!sc.isRunning()) { throw new AssertionError(
                                                    "Server["
                                                        + serverControl[index].getTsaPort()
                                                        + "] is not running as expected. State Found:[STOPPED] Expected:[RUNNING]!"); }
    sc.shutdown();
    stopL2GroupProxy(index);
    expectedServerRunning[index] = false;
    if (active) {
      stopL1Proxy(index);
      if (expectedRunningServerCount() > 0) {
        startL1ProxyOnActiveServerAsync();
      }
    }

    System.out.println("*** Server stopped [" + serverControl[index].getTsaPort() + "]");
  }

  private void stopL2GroupProxy(int index) {
    if (isProxyTsaGroupPort()) {
      proxyL2Managers[index].proxyDown();
    }
  }

  private void stopL1Proxy(int index) {
    if (isProxyTsaPort()) {
      System.out.println("*** stopping the DSO proxy with proxy port as " + proxyL1Managers[index].getProxyPort()
                         + " and DSO port as " + proxyL1Managers[index].getTsaPort());
      proxyL1Managers[index].proxyDown();
    }
  }

  private void cleanupServerDB(int index) throws Exception {
    if (testConfig.getRestartable()) {
      if (renameDataDir) {
        System.out.println("Moving data directory for server=[" + serverControl[index].getTsaPort() + "]");
        renameDir(groupData.getDataDirectoryPath(index));
      } else {
        System.out.println("Deleting data directory for server=[" + serverControl[index].getTsaPort() + "]");
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
    closeJMXConnector(index);
    MBeanServerConnection mBeanServer;
    synchronized (jmxConnectors) {
      jmxConnectors[index] = getJMXConnector(serverControl[index].getAdminPort());
      mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    }
    return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                         TCServerInfoMBean.class, true);
  }

  private static JMXConnector getJMXConnector(int jmxPort) throws IOException {
    return JMXUtils.getJMXConnector("localhost", jmxPort);
  }

  private DSOMBean getDsoMBean(int index) throws IOException {
    JMXConnector jmxc = getJMXConnector(serverControl[index].getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DSOMBean dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    return dsoMBean;
  }

  private DGCMBean getLocalDGCMBean(int index) throws IOException {
    JMXConnector jmxc = getJMXConnector(serverControl[index].getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DGCMBean dgcMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.LOCAL_DGC_STATS,
                                                                      DGCMBean.class, false);
    return dgcMBean;
  }

  private void closeJMXConnector(int i) {
    synchronized (jmxConnectors) {
      if (jmxConnectors[i] != null) {
        try {
          jmxConnectors[i].close();
        } catch (Exception e) {
          System.out.println("JMXConnector for server=[" + serverControl[i].getTsaPort() + "] already closed.");
          e.printStackTrace();
        }
        jmxConnectors[i] = null;
      }
    }
  }

  private void closeJMXConnectors() {
    for (int i = 0; i < jmxConnectors.length; i++) {
      final int n = i;
      // Close JMX connectors in a separate thread, to avoid test timeouts in case the server has already crashed
      // and won't be coming back
      Thread t = new Thread() {
        @Override
        public void run() {
          closeJMXConnector(n);
        }
      };
      t.setDaemon(true);
      t.start();
      // Wait if necessary up to 1 sec, for good measure
      try {
        t.join(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  List<DSOMBean> connectAllDsoMBeans() {
    List<DSOMBean> mbeans = new ArrayList<DSOMBean>();
    for (int i = 0; i < serverControl.length; i++) {
      try {
        mbeans.add(getDsoMBean(i));
      } catch (IOException e) {
        System.out.println("XXXXXXX could not connect to server[" + serverControl[i].getTsaPort() + "], jmxPort:"
                           + serverControl[i].getAdminPort());
      }
    }
    return mbeans;
  }

  List<DGCMBean> connectAllLocalDGCMBeans() {
    List<DGCMBean> mbeans = new ArrayList<DGCMBean>();
    for (int i = 0; i < serverControl.length; i++) {
      try {
        mbeans.add(getLocalDGCMBean(i));
      } catch (IOException e) {
        System.out.println("XXXXXXX could not connect to server[" + serverControl[i].getTsaPort() + "], jmxPort:"
                           + serverControl[i].getAdminPort());
      }
    }
    return mbeans;
  }

  GroupsData getGroupData() {
    return groupData;
  }

  /**
   * crash active server and wait for passive server to take over. If Passive is present.
   */
  synchronized void crashActiveAndWaitForPassiveToTakeOver() throws Exception {
    if (stopped) return;
    crashActive();
    if (expectedRunningServerCount() > 0) {
      // wait for passive to take over only If passive was running.
      int activeServer = getActiveServerIndex();
      while (activeServer < 0 && !stopped) {
        ThreadUtil.reallySleep(1000);
        activeServer = getActiveServerIndex();
      }
    }
    System.out.println("******* Done Crashing active server");

  }

  synchronized void crashActive() throws Exception {
    int activeIndex = getActiveServerIndex();
    if (activeIndex < 0) { throw new AssertionError("Trying to crash active server when no active server is present"); }
    crashServer(activeIndex);
    System.out.println("******* Done Crashing active server");
  }

  synchronized int getActiveServerIndex() {
    System.out.println("Searching for active server... ");
    for (int index = 0; index < groupData.getServerCount(); index++) {
      if (isActive(index)) { return index; }
    }
    return -1;

  }

  // public void pauseActive(long pauseTimeMillis) throws InterruptedException {
  // int activeIndex = getActiveServerIndex();
  // if (activeIndex < 0) {
  // System.out.println("Trying to pause active server when no active server is present");
  // return;
  // }
  // pauseServer(activeIndex, pauseTimeMillis);
  // }
  //
  // void pausePassive(int index, long pauseTimeMillis) throws InterruptedException {
  // int activeIndex = getActiveServerIndex();
  // if (index == activeIndex) { return; }
  // pauseServer(index, pauseTimeMillis);
  // }

  void pauseServer(final int index, final long pauseTimeMillis) throws InterruptedException {
    if (isExpectedServerRunning(index)) {
      serverControl[index].pauseServer(pauseTimeMillis);
    }
    System.out.println("******* Server Resume After Pause");
  }

  void pauseServer(final int index) throws InterruptedException {
    if (isExpectedServerRunning(index)) {
      serverControl[index].pauseServer();
    }
    System.out.println("******* Server Paused Index : " + index);
  }

  void unpauseServer(final int index) throws InterruptedException {
    if (isExpectedServerRunning(index)) {
      serverControl[index].unpauseServer();
    }
    System.out.println("******* Server UnPaused Index : " + index);
  }

  private synchronized boolean isExpectedServerRunning(int index) {
    return expectedServerRunning[index];
  }

  void crashAllPassive() throws Exception {
    System.out.println("**** Crashing all passives");
    int activeIndex = getActiveServerIndex();
    for (int i = 0; i < groupData.getServerCount(); i++) {
      if (i != activeIndex && expectedServerRunning[i]) {
        crashServer(i, true);
      }
    }
    System.out.println("***** Done Crashing all passives");

  }

  synchronized void crashPassive(int passiveToCrash) throws Exception {
    verifyIndex(passiveToCrash);
    if (isActive(passiveToCrash)) { throw new AssertionError("**** Trying to crash server ["
                                                             + serverControl[passiveToCrash].getTsaPort()
                                                             + "] as passive server but it is in ACTIVE state."); }
    crashServer(passiveToCrash, true);
  }

  synchronized void crashRandomServer() throws Exception {
    if (stopped) return;

    if (random == null) { throw new AssertionError("Random number generator was not set."); }

    debugPrintln("***** Choosing random server... ");

    int crashIndex = random.nextInt(groupData.getServerCount());
    if (expectedServerRunning[crashIndex]) {
      crashServer(crashIndex);
    }
  }

  void crashServer(int index) throws Exception {
    crashServer(index, false);
  }

  private synchronized void crashServer(int index, boolean ignoreState) throws Exception {
    System.out.println("******** Crashing Server " + index);

    boolean active = isActive(index);
    System.out.println("Crashing server: dsoPort=[" + serverControl[index].getTsaPort() + "]");
    if (active && !ignoreState && expectedRunningServerCount() > 1) {
      waituntilPassiveStandBy();
    }
    ServerControl server = serverControl[index];
    server.crash();
    debugPrintln("***** Sleeping after crashing server ");
    waitForServerCrash(server);
    stopL2GroupProxy(index);
    expectedServerRunning[index] = false;
    // If active server is crashed. stop l1 proxy and start it on the new active in async thread.
    if (active) {
      stopL1Proxy(index);
      if (expectedRunningServerCount() > 1) {
        startL1ProxyOnActiveServerAsync();
      }
    }
    debugPrintln("***** Done sleeping after crashing server ");
    lastCrashedIndex = index;
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");
    if (expectedRunningServerCount() > 0 && testConfig.getCrashConfig().shouldCleanDbOnCrash()) {
      cleanupServerDB(lastCrashedIndex);
    }
  }

  synchronized void restartLastCrashedServer() throws Exception {
    if (stopped) return;
    debugPrintln("*****  restarting last crashed server");

    if (lastCrashedIndex >= 0) {
      if (serverControl[lastCrashedIndex].isRunning()) { throw new AssertionError(
                                                                                  "Server["
                                                                                      + serverControl[lastCrashedIndex]
                                                                                          .getTsaPort()
                                                                                      + "] is not down as expected!"); }
      restartCrashedServer(lastCrashedIndex);
    } else {
      throw new AssertionError("No crashed servers to restart.");
    }
  }

  void restartCrashedServer(int serverIndex) throws Exception {

    debugPrintln("*****  restarting crashed server");

    if (serverControl[serverIndex].isRunning()) { throw new AssertionError("Server["
                                                                           + serverControl[serverIndex].getTsaPort()
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

  boolean isServerRunning(int index) {
    return serverControl[index] != null && serverControl[index].isRunning();
  }

  void waituntilPassiveStandBy() throws Exception {
    while (!isPassiveStandBy() && !stopped) {
      Thread.sleep(1000);
    }
  }

  void waituntilPassiveStandByIfNecessary() throws Exception {
    if (expectedRunningServerCount() == 1) { return; }
    waituntilPassiveStandBy();
  }

  void waituntilEveryPassiveStandBy() throws Exception {
    while (!isEveryPassiveStandBy() && !stopped) {
      Thread.sleep(1000);
    }
  }

  void waitUntilActive() throws Exception {
    while (!isActivePresent() && !stopped) {
      Thread.sleep(1000);
    }
  }

  private boolean isProxyTsaGroupPort() {
    return testConfig.getL2Config().isProxyTsaGroupPorts();
  }

  private boolean isProxyTsaPort() {
    return testConfig.getL2Config().isProxyTsaPorts();
  }

  boolean dumpClusterState() throws Exception {
    boolean rv = true;
    for (int i = 0; i < getGroupData().getServerCount(); i++) {
      if (!dumpClusterStateInternal(i)) rv = false;
    }
    return rv;
  }

  private boolean dumpClusterStateInternal(int serverIndex) throws IOException, InterruptedException, Exception {
    if (serverControl[serverIndex].isRunning()) {
      System.out.println("Dumping server=[" + serverControl[serverIndex].getTsaPort() + "]");

      MBeanServerConnection mbs;
      synchronized (jmxConnectors) {
        try {
          if (jmxConnectors[serverIndex] == null) {
            jmxConnectors[serverIndex] = getJMXConnector(serverControl[serverIndex].getAdminPort());
          }
          mbs = jmxConnectors[serverIndex].getMBeanServerConnection();
        } catch (IOException ioe) {
          System.out.println("Need to recreate jmxConnector for server=[" + serverControl[serverIndex].getTsaPort()
                             + "]...");
          jmxConnectors[serverIndex] = getJMXConnector(serverControl[serverIndex].getAdminPort());
          mbs = jmxConnectors[serverIndex].getMBeanServerConnection();
        }
      }

      L2DumperMBean mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                          L2DumperMBean.class, true);
      try {
        mbean.dumpClusterState();
        return true;
      } catch (Exception e) {
        System.out.println("Could not find L2DumperMBean....");
        System.out.println("Thread dumping server=[" + serverControl[serverIndex].getTsaPort() + "] ");
        mbean.doThreadDump();
      }

    }

    closeJMXConnectors();
    return false;
  }

  synchronized boolean isActivePresent() {
    return getActiveServerIndex() == -1 ? false : true;
  }

  private boolean isEveryPassiveStandBy() {

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
        System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[i].getTsaPort() + "]... ["
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

  boolean isPassiveStandBy() {

    System.out.println("Searching for appropriate passive server(s)... ");
    for (int i = 0; i < groupData.getServerCount(); i++) {
      try {
        if (tcServerInfoMBeans[i].isPassiveStandby()) { return true; }
      } catch (Exception e) {
        System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[i].getTsaPort() + "]... ["
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

  boolean isPassiveUninitialized(int index) {
    boolean isPassiveUnitialized = false;
    try {
      isPassiveUnitialized = tcServerInfoMBeans[index].isPassiveUninitialized();
    } catch (Exception e) {
      System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[index].getTsaPort() + "]...");
      try {
        tcServerInfoMBeans[index] = getTcServerInfoMBean(index);
        isPassiveUnitialized = tcServerInfoMBeans[index].isPassiveUninitialized();
      } catch (Exception e2) {
        System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
      }
    }
    return isPassiveUnitialized;
  }

  private boolean isActive(int index) {
    boolean isActive = false;
    try {
      isActive = tcServerInfoMBeans[index].isActive();
    } catch (Exception e) {
      System.out.println("Need to fetch tcServerInfoMBean for server=[" + serverControl[index].getTsaPort() + "]...");
      try {
        tcServerInfoMBeans[index] = getTcServerInfoMBean(index);
        isActive = tcServerInfoMBeans[index].isActive();
      } catch (Exception e2) {
        System.out.println("exception restoring jmx connection [" + e2.getMessage() + "]");
      }
    }
    return isActive;
  }

  void startCrasher() {
    if (crasherStarted.compareAndSet(false, true)) {
      if (!testConfig.getCrashConfig().getCrashMode().equals(ServerCrashMode.NO_CRASH)) {
        crasherThread = new Thread(serverCrasher, "server-crasher");
        crasherThread.setDaemon(true);
        crasherThread.start();
      }
    } else {
      throw new AssertionError("server Crasher already started");
    }
  }

  private void stopCrasher() throws InterruptedException {
    this.serverCrasher.stop();
    if (crasherStarted.get() && !testConfig.getCrashConfig().getCrashMode().equals(ServerCrashMode.NO_CRASH)) {
      crasherThread.join();
    }
  }

  synchronized void closeTsaProxyOnActiveServer() {
    if (isProxyTsaPort()) {
      int activeServerIndex = getActiveServerIndex();
      System.out.println("*** stopping the DSO proxy with proxy port as "
                         + proxyL1Managers[activeServerIndex].getProxyPort() + " and DSO port as "
                         + proxyL1Managers[activeServerIndex].getTsaPort());
      proxyL1Managers[activeServerIndex].closeClientConnections();
    }
  }

  int waitForServerExit(int serverIndex) throws Exception {
    return serverControl[serverIndex].waitFor();
  }

  synchronized void stopTsaProxyOnActiveServer() {
    if (isProxyTsaPort()) {
      int activeServerIndex = getActiveServerIndex();
      stopL1Proxy(activeServerIndex);
    }
  }

}

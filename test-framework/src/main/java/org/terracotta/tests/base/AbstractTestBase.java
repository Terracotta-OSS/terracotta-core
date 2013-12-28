package org.terracotta.tests.base;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.logging.TCLogging;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandler;
import com.tc.test.runner.TcTestRunner;
import com.tc.test.runner.TcTestRunner.Configs;
import com.tc.test.setup.GroupsData;
import com.tc.test.setup.TestJMXServerManager;
import com.tc.test.setup.TestServerManager;
import com.tc.text.Banner;
import com.tc.util.PortChooser;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;

@RunWith(value = TcTestRunner.class)
public abstract class AbstractTestBase extends TCTestCase implements TestFailureListener {
  private static final String              TC_TESTS_INFO_STANDALONE  = "tc.tests.info.standalone";
  private static final String              DEFAULT_CONFIG            = "default-config";
  public static final String               TC_CONFIG_PROXY_FILE_NAME = "tc-config-proxy.xml";
  protected static final String            SEP                       = File.pathSeparator;
  private final String                     TC_CONFIG_FILE_NAME       = "tc-config.xml";
  private final TestConfig                 testConfig;
  private final File                       tcConfigFile;
  private final File                       tcConfigProxyFile;
  protected TestServerManager              testServerManager;
  protected final File                     tempDir;
  protected File                           javaHome;
  protected TestClientManager              clientRunner;
  private volatile TestJMXServerManager    jmxServerManager;
  protected volatile Thread                duringRunningClusterThread;
  protected volatile Thread                testExecutionThread;
  private static final String              log4jPrefix               = "log4j.logger.";
  private final Map<String, LogLevel>      tcLoggingConfigs          = new HashMap<String, LogLevel>();
  protected final AtomicReference<Throwable> testException             = new AtomicReference<Throwable>();
  protected volatile PauseManager                     pauseManager;

  public AbstractTestBase(TestConfig testConfig) {
    this.testConfig = testConfig;
    try {
      this.tempDir = getTempDirectory();
      FileUtils.forceMkdir(tempDir);
      FileUtils.cleanDirectory(tempDir);
      tcConfigFile = getTempFile(TC_CONFIG_FILE_NAME);
      tcConfigProxyFile = getTempFile(TC_CONFIG_PROXY_FILE_NAME);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (Vm.isJRockit()) {
      testConfig.getClientConfig().addExtraClientJvmArg("-XXfullSystemGC");
    }
    testConfig.getClientConfig().addExtraClientJvmArg("-XX:+HeapDumpOnOutOfMemoryError");
    if (Boolean.getBoolean("com.tc.test.toolkit.devmode")) {
      testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.test.toolkit.devmode=true");
    }

    if (Boolean.parseBoolean(TestConfigObject.getInstance().getProperty(TC_TESTS_INFO_STANDALONE))) {
      testConfig.setStandAloneTest(true);
      testConfig.getClientConfig().addExtraClientJvmArg("-D" + TC_TESTS_INFO_STANDALONE + "=true");
      String tsaPort = TestConfigObject.getInstance().getProperty("tc.tests.info.tsa.port");
      testConfig.getClientConfig().addExtraClientJvmArg("-Dtc.tests.info.tsa.port=" + tsaPort);
    }
    // disable java awt pop-up for mac os x
    testConfig.getClientConfig().addExtraClientJvmArg("-Djava.awt.headless=true");
    testConfig.getL2Config().addExtraServerJvmArg("-Djava.awt.headless=true");
    // testConfig.getClientConfig().addExtraClientJvmArg("-Dapple.awt.UIElement=true");
    // testConfig.getL2Config().addExtraServerJvmArg("-Dapple.awt.UIElement=true");
  }

  /**
   * Returns the list of test configs the test has to run with.
   * Override this method to run the same test with multiple
   * configs
   */
  @Configs
  public static List<TestConfig> getTestConfigs() {
    TestConfig testConfig = new TestConfig(DEFAULT_CONFIG);
    testConfig.getGroupConfig().setMemberCount(1);
    TestConfig[] testConfigs = new TestConfig[] { testConfig };
    return Arrays.asList(testConfigs);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    if (testConfig.isPauseFeatureEnabled() && !Os.isUnix()) {
      disableTest();
    }
    if (!"".equals(System.getProperty("com.tc.productkey.path"))) {
      if (!testConfig.getL2Config().isOffHeapEnabled() && testConfig.getL2Config().isAutoOffHeapEnable()) {
        System.out.println("============= Offheap is turned off, switching it on to avoid OOMEs! ==============");
        testConfig.getL2Config().setOffHeapEnabled(true);
        if (testConfig.getGroupConfig().getMemberCount() * testConfig.getNumOfGroups() < 4) {
          testConfig.getL2Config().setDirectMemorySize(1024);
          testConfig.getL2Config().setMaxOffHeapDataSize(512);
        } else {
          boolean isRestartable = testConfig.getRestartable();
          // reduce memory settings for AA tests until RAM is increased on MNK machines.
          TestBaseUtil.configureOffHeap(testConfig, 1024, 300);
          testConfig.setRestartable(isRestartable);
        }
      } else {
        Banner.warnBanner("Offheap is disabled and auto-enable-offheap is also set to false! L2 may suffer OOME");
      }
    } else {
      if (testConfig.getRestartable()) {
        System.out.println("============== Disabling opensource restartable tests ===============");
        disableTest();
      }
    }

    tcTestCaseSetup();

    if (testWillRun) {
      try {
        System.out.println("***************" + Calendar.getInstance().getTime() + " Starting Test with Test Profile : "
                           + testConfig.getConfigName() + " **************************");

        jmxServerManager = new TestJMXServerManager(new PortChooser().chooseRandomPort());
        jmxServerManager.startJMXServer();

        setJavaHome();
        pauseManager = setupPauseManager();
        clientRunner = setupTestClientManager();
        pauseManager.setClientManager(clientRunner); // Adds a circular dependency
        if (!testConfig.isStandAloneTest()) {
          testServerManager = setupTestServerManager();
          pauseManager.setTestServerManager(testServerManager);
          writeProxyTcConfigFile();
          startServers();
        }

        TestHandler testHandlerMBean = new TestHandler(testServerManager, clientRunner, testConfig, pauseManager);
        jmxServerManager.registerMBean(testHandlerMBean, TestHandler.TEST_SERVER_CONTROL_MBEAN);
        configureTestHandlerMBean(testHandlerMBean);
        executeDuringRunningCluster();
      } catch (Throwable e) {
        e.printStackTrace();
        throw new AssertionError(e);
      }
    }
  }

  protected TestServerManager setupTestServerManager() throws Exception {
    return new TestServerManager(testConfig, tempDir, tcConfigFile, javaHome, this);
  }

  protected PauseManager setupPauseManager() {
    return new PauseManager(testConfig);
  }

  protected TestClientManager setupTestClientManager() throws IOException {
    return new TestClientManager(tempDir, this, testConfig, pauseManager);
  }

  private void writeProxyTcConfigFile() throws Exception {
    if (testConfig.getL2Config().isProxyTsaPorts()) {
      FileUtils.writeStringToFile(tcConfigProxyFile, testServerManager.getTsaProxyConfig());
    }
  }

  protected void configureTestHandlerMBean(TestHandler testHandler) {
    // empty method - override if necessary
  }

  protected void startServers() throws Exception {
    testServerManager.startAllServers();
  }

  protected void setJavaHome() {
    if (javaHome == null) {
      String javaHome_local = getTestConfigObject().getL2StartupJavaHome();
      if (javaHome_local == null) { throw new IllegalStateException(TestConfigObject.L2_STARTUP_JAVA_HOME
                                                                    + " must be set to a valid JAVA_HOME"); }
      javaHome = new File(javaHome_local);
    }
  }

  protected TestConfigObject getTestConfigObject() {
    return TestConfigObject.getInstance();
  }

  @Override
  @Test
  public void runTest() throws Throwable {
    if (!testWillRun) return;

    testExecutionThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          startClients();
          postClientVerification();
        } catch (Throwable throwable) {
          testException.compareAndSet(null, throwable);
        }
      }
    }, "Test execution thread");
    testExecutionThread.setDaemon(true);
    testExecutionThread.start();
    try {
      pauseManager.startServerPauseTasks();
      testExecutionThread.join();
    } catch (InterruptedException e) {
      testExecutionThread.interrupt(); // stop the test execution thread.
      throw new RuntimeException("Test timed out");
    }

    tcTestCaseTearDown(testException.get());
  }

  @Override
  public void testFailed(String reason) {
    if (testExecutionThread != null) {
      doDumpServerDetails();
      testException.compareAndSet(null, new Throwable(reason));
      testExecutionThread.interrupt();
    }
  }

  /**
   * @return the port number on which the TestHandler Mbean can be connected
   */
  public int getTestControlMbeanPort() {
    return this.jmxServerManager.getJmxServerPort();
  }

  /**
   * returns the testConfig with which this test is running
   * 
   * @return : the test config with which the test is running
   */
  protected TestConfig getTestConfig() {
    return this.testConfig;
  }

  protected abstract String createClassPath(Class client) throws IOException;

  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    if ((exitCode != 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

    FileReader fr = null;
    try {
      fr = new FileReader(output);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains("[PASS: " + clientName + "]")) {
          reader.close();
          fr.close();
          return;
        }
      }
      throw new AssertionError("Client " + clientName + " did not pass");
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
      } catch (Exception e) {
        //
      }
    }
  }

  protected void preStart(File workDir) {
    //
  }

  /**
   * Override this method if there is a need to do some verification when the clients are done
   */
  protected void postClientVerification() throws Exception {
    //
  }

  protected String getTestDependencies() {
    return "";
  }

  protected String makeClasspath(String... jars) {
    String cp = TestBaseUtil.constructClassPath(jars);

    if (!tcLoggingConfigs.isEmpty()) {
      return addToClasspath(cp, getTCLoggingFilePath());
    } else {
      return cp;
    }
  }

  protected String makeClasspath(List<String> list, String... jars) {
    List<String> fullList = new ArrayList<String>(list);
    Collections.addAll(fullList, jars);
    return makeClasspath(fullList.toArray(new String[0]));
  }

  private String getTCLoggingFilePath() {
    File log4jPropFile = null;
    BufferedWriter writer = null;
    try {
      log4jPropFile = new File(getTempDirectory(), TCLogging.LOG4J_PROPERTIES_FILENAME);
      writer = new BufferedWriter(new FileWriter(log4jPropFile));
      for (Entry<String, LogLevel> entry : tcLoggingConfigs.entrySet()) {
        writer.write(log4jPrefix + entry.getKey() + "=" + entry.getValue().name() + "\n");
      }
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    } finally {
      try {
        writer.close();
      } catch (IOException e1) {
        throw new IllegalStateException(e1.getMessage());
      }
    }
    return log4jPropFile.getParent();
  }

  protected String addToClasspath(String cp, String path) {
    return cp + SEP + path;
  }

  protected List<String> getExtraJars() {
    return Collections.emptyList();
  }

  protected void configureTCLogging(String className, LogLevel LogLevel) {
    tcLoggingConfigs.put(className, LogLevel);
  }

  protected String getTerracottaURL() {
    return TestBaseUtil.getTerracottaURL(getGroupsData(), false);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    if (testWillRun) {
      System.out.println("Waiting for During Cluster running thread to finish");
      duringRunningClusterThread.join();
      if (!testConfig.isStandAloneTest()) {
        stopServers();
      }
      jmxServerManager.stopJmxServer();
      System.out.println("***************" + Calendar.getInstance().getTime() + " Stopped Test with Test Profile : "
                         + testConfig.getConfigName() + " **************************");
    }
  }

  protected void stopServers() throws Exception {
    this.testServerManager.stopAllServers();
  }

  @Override
  protected File getTempDirectory() throws IOException {
    // this is a hack but there is no direct way to know whether a test is going to be run with single config
    if (testConfig.getConfigName().equals(DEFAULT_CONFIG)) { return super.getTempDirectory(); }

    File tempDirectory = new File(super.getTempDirectory(), testConfig.getConfigName());
    return tempDirectory;
  }

  @Override
  protected File getTempFile(String fileName) throws IOException {
    return new File(getTempDirectory(), fileName);
  }

  @Override
  protected boolean cleanTempDir() {
    return false;
  }

  @Override
  protected void doDumpServerDetails() {
    testServerManager.dumpClusterState(getThreadDumpCount(), getThreadDumpInterval());
  }

  protected void startClients() throws Throwable {
    int index = 0;
    Runner[] runners = testConfig.getClientConfig().isParallelClients() ? new Runner[testConfig.getClientConfig()
        .getClientClasses().length] : new Runner[] {};
    for (Class<? extends Runnable> c : testConfig.getClientConfig().getClientClasses()) {
      if (!testConfig.getClientConfig().isParallelClients()) {
        runClient(c);
      } else {
        Runner runner = new Runner(c);
        runners[index++] = runner;
        runner.start();
      }
    }

    for (Runner runner : runners) {
      runner.finish();
    }
  }

  protected void runClient(Class client) throws Throwable {
    List<String> emptyList = Collections.emptyList();
    runClient(client, client.getSimpleName(), emptyList);
  }

  protected void runClient(Class client, String clientName, List<String> extraClientArgs) throws Throwable {
    clientRunner.runClient(client, clientName, extraClientArgs);
  }

  public GroupsData getGroupData(final int groupIndex) {
    return this.testServerManager.getGroupData(groupIndex);
  }

  public GroupsData[] getGroupsData() {
    return this.testServerManager.getGroupsData();
  }

  protected void duringRunningCluster() throws Exception {
    // do not delete this method, it is used by tests that override it
  }

  public File makeTmpDir(Class klass) {
    File tmp_dir_root = new File(getTestConfigObject().tempDirectoryRoot());
    File tmp_dir = new File(tmp_dir_root, ClassUtils.getShortClassName(klass));
    tmp_dir.mkdirs();
    return tmp_dir;
  }

  private void executeDuringRunningCluster() {
    duringRunningClusterThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          duringRunningCluster();
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
    });
    duringRunningClusterThread.setName(getClass().getName() + " duringRunningCluster");
    duringRunningClusterThread.start();
  }

  protected class Runner extends Thread {

    private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    private final Class                      clientClass;

    public Runner(Class clientClass) {
      this.clientClass = clientClass;
    }

    @Override
    public void run() {
      try {
        runClient(clientClass);
      } catch (Throwable t) {
        error.set(t);
      }
    }

    public void finish() throws Throwable {
      join();
      Throwable t = error.get();
      if (t != null) throw t;
    }
  }

  /**
   * Disables the test if the total physical memory on the machine is lower that the specified value
   * 
   * @param physicalMemory memory in gigs below which the test should not run on the machine
   */
  @SuppressWarnings("restriction")
  protected void disableIfMemoryLowerThan(int physicalMemory) {
    try {
      if (getTotalPhysicalMemory() < physicalMemory) {
        disableTest();
      }
    } catch (Exception e) {
      System.out
          .println("WARNING: test may fail because we are not able to determine the system memory and it may be < "
                   + physicalMemory + " GB");
      e.printStackTrace();
    }

  }

  /**
   * returns Total physical Memory in GB or throws Exception if it not able to determine the physical memory
   */
  public long getTotalPhysicalMemory() throws Exception {
    long gb = 1024 * 1024 * 1024;
    long totalAvailableMem = -1l;
    Class clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
    MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
    OperatingSystemMXBean osMBean = (OperatingSystemMXBean) ManagementFactory
        .newPlatformMXBeanProxy(mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, clazz);
    Method method = osMBean.getClass().getMethod("getTotalPhysicalMemorySize", new Class[] {});
    long totalBytes = (Long) method.invoke(osMBean, (Object[]) null);
    System.out.println("XXXXX total mem: " + totalBytes);
    totalAvailableMem = totalBytes / gb;
    return totalAvailableMem;
  }

  public File getTcConfigFile() {
    return tcConfigFile;
  }

  protected void stopClient(final int index) {
    this.clientRunner.stopClient(index);
  }

  public File getTsaProxyConfigFile() {
    return tcConfigProxyFile;
  }

  public TestJMXServerManager getJmxServerManager() {
    return jmxServerManager;
  }
}

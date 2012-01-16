package org.terracotta.tests.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.exception.ImplementMe;
import com.tc.test.TestConfigObject;
import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandler;
import com.tc.test.jmx.TestHandlerMBean;
import com.tc.test.runner.TcTestRunner;
import com.tc.test.runner.TcTestRunner.Configs;
import com.tc.test.setup.GroupsData;
import com.tc.test.setup.TestJMXServerManager;
import com.tc.test.setup.TestServerManager;
import com.tc.util.PortChooser;

@RunWith(value = TcTestRunner.class)
public abstract class AbstractTestBase extends TerracottaTestCase {
  protected static final String SEP     = File.pathSeparator;
  private final TestConfig      testConfig;
  private final File            tcConfigFile;
  protected TestServerManager   testServerManager;
  protected final File          tempDir;
  protected File                javaHome;
  private TestClientManager     clientRunner;
  private volatile boolean      runTest = false;
  private TestJMXServerManager  jmxServerManager;
  private Thread                duringRunningClusterThread;

  public AbstractTestBase(TestConfig testConfig) {
    this.testConfig = testConfig;
    try {
      this.tempDir = getTempDirectory();
      tempDir.mkdir();
      FileUtils.cleanDirectory(tempDir);
      tcConfigFile = getTempFile("tc-config.xml");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Configs
  public static List<TestConfig> getTestConfigs() {
    TestConfig testConfig = new TestConfig("SingleServerConfig");
    testConfig.getGroupConfig().setMemberCount(1);
    TestConfig[] testConfigs = new TestConfig[] { testConfig };
    return Arrays.asList(testConfigs);
  }

  @Before
  public void setUp() throws Exception {
    runTest = preRun();
    Assume.assumeTrue(runTest);
    try {
      System.out.println("*************** Starting Test with Test Profile : " + testConfig.getConfigName()
                         + " **************************");
      setJavaHome();
      testServerManager = new TestServerManager(this.testConfig, this.tempDir, this.tcConfigFile, this.javaHome);
      clientRunner = new TestClientManager(tempDir, this, this.testConfig);
      startServers();
      TestHandlerMBean testHandlerMBean = new TestHandler(testServerManager, testConfig);
      jmxServerManager = new TestJMXServerManager(new PortChooser().chooseRandomPort(), testHandlerMBean);
      jmxServerManager.startJMXServer();
      executeDuringRunningCluster();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new AssertionError(e);
    }
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

  @Test
  public void runTest() throws Throwable {
    clientRunner.runClients();
  }

  public int getTestControlMbeanPort() {
    return this.jmxServerManager.getJmxServerPort();
  }

  protected TestConfig getTestConfig() {
    return this.testConfig;
  }

  protected abstract String createClassPath(Class client, boolean withStandaloneJar) throws IOException;

  protected String getEhcacheTerracotta() {
    throw new ImplementMe(
                          "The sub class needs to define this method if it needs to add ehcache terracotta jar in the classpath");
  }

  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    if ((exitCode != 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

    FileReader fr = null;
    try {
      fr = new FileReader(output);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains("[PASS: " + clientName + "]")) return;
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

  protected String getTestDependencies() {
    return "";
  }

  protected String makeClasspath(String... jars) {
    String cp = "";
    for (String jar : jars) {
      cp += SEP + jar;
    }

    for (String extra : getExtraJars()) {
      cp += SEP + extra;
    }

    return cp;
  }

  protected String addToClasspath(String cp, String path) {
    return cp + SEP + path;
  }

  protected List<String> getExtraJars() {
    return Collections.emptyList();
  }

  protected String getTerracottaURL() {
    return TestBaseUtil.getTerracottaURL(getGroupsData());
  }

  @After
  public void tearDown() throws Exception {
    if (runTest) {
      System.out.println("Waiting for During Cluster running thread to finish");
      duringRunningClusterThread.join();
      this.testServerManager.stopAllServers();
      this.jmxServerManager.stopJmxServer();
      System.out.println("*************** Stopped Test with Test Profile : " + testConfig.getConfigName()
                         + " **************************");
    }
  }

  @Override
  protected File getTempDirectory() throws IOException {
    File tempDirectory = new File(super.getTempDirectory(), testConfig.getConfigName());
    return tempDirectory;
  }

  protected File getTempFile(String fileName) throws IOException {
    return new File(getTempDirectory(), fileName);
  }

  protected boolean cleanTempDir() {
    return false;
  }

  @Override
  protected void doDumpServerDetails() {
    testServerManager.dumpClusterState(getThreadDumpCount(), getThreadDumpInterval());
  }

  protected void runClient(Class client, boolean withStandaloneJar) throws Throwable {
    clientRunner.runClient(client, withStandaloneJar);
  }

  protected void runClient(Class client) throws Throwable {
    clientRunner.runClient(client);
  }

  protected void runClient(Class client, boolean withStandaloneJar, String clientName, List<String> extraClientArgs)
      throws Throwable {
    clientRunner.runClient(client, withStandaloneJar, clientName, extraClientArgs);
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

}

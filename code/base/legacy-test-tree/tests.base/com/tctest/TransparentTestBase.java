/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.CopyUtils;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.test.ProcessInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerConfigCreator;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.test.restart.ServerCrasher;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.runtime.ThreadDump;
import com.tctest.runner.DistributedTestRunner;
import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.TestGlobalIdGenerator;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.AssertionFailedError;

public abstract class TransparentTestBase extends BaseDSOTestCase implements TransparentTestIface, TestConfigurator {

  public static final int                         DEFAULT_CLIENT_COUNT            = 2;
  public static final int                         DEFAULT_INTENSITY               = 10;
  public static final int                         DEFAULT_VALIDATOR_COUNT         = 0;
  public static final int                         DEFAULT_ADAPTED_MUTATOR_COUNT   = 0;
  public static final int                         DEFAULT_ADAPTED_VALIDATOR_COUNT = 0;

  private TestTVSConfigurationSetupManagerFactory configFactory;
  private DSOClientConfigHelper                   configHelper;
  protected DistributedTestRunner                 runner;
  private DistributedTestRunnerConfig             runnerConfig                    = new DistributedTestRunnerConfig(
                                                                                                                    getTimeoutValueInSeconds());
  private TransparentAppConfig                    transparentAppConfig;
  private ApplicationConfigBuilder                possibleApplicationConfigBuilder;

  private String                                  mode;
  private ServerControl                           serverControl;
  private ServerControl[]                         serverControls;
  private TCPProxy[]                              proxies;
  private boolean                                 controlledCrashMode             = false;
  private ServerCrasher                           crasher;
  private File                                    javaHome;
  private int                                     pid                             = -1;

  // for active-passive tests
  private ActivePassiveServerManager              apServerManager;
  private ActivePassiveTestSetupManager           apSetupManager;
  private TestState                               crashTestState;

  protected TestConfigObject getTestConfigObject() {
    return TestConfigObject.getInstance();
  }

  protected void setJavaHome() {
    if (javaHome == null) {
      String javaHome_local = getTestConfigObject().getL2StartupJavaHome();
      if (javaHome_local == null) { throw new IllegalStateException(TestConfigObject.L2_STARTUP_JAVA_HOME
                                                                    + " must be set to a valid JAVA_HOME"); }
      javaHome = new File(javaHome_local);
    }
  }

  protected void setJvmArgsL1Reconnect(ArrayList jvmArgs) {
    System.setProperty("com.tc.l1.reconnect.enabled", "true");
    TCPropertiesImpl.setProperty("l1.reconnect.enabled", "true");

    jvmArgs.add("-Dcom.tc.l1.reconnect.enabled=true");
  }

  protected void setUp() throws Exception {
    setUp(configFactory(), configHelper());

    // config should be set up before tc-config for external L2s are written out
    setupConfig(configFactory());

    if (canRunProxyConnect() && !enableL1Reconnect()) { throw new AssertionError(
                                                                                 "proxy-connect needs l1reconnect enabled, please overwrite enableL1Reconnect()"); }

    ArrayList jvmArgs = new ArrayList();
    // for some test cases to enable l1reconnect
    if (enableL1Reconnect()) {
      setJvmArgsL1Reconnect(jvmArgs);
    }

    RestartTestHelper helper = null;
    PortChooser portChooser = new PortChooser();
    if ((isCrashy() && canRunCrash()) || useExternalProcess()) {
      // javaHome is set here only to enforce that java home is defined in the test config
      // javaHome is set again inside RestartTestEnvironment because how that class is used
      // TODO: clean this up
      setJavaHome();

      helper = new RestartTestHelper(mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH),
                                     new RestartTestEnvironment(getTempDirectory(), portChooser,
                                                                RestartTestEnvironment.PROD_MODE, configFactory()),
                                     jvmArgs);
      int dsoPort = helper.getServerPort();
      int adminPort = helper.getAdminPort();
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
      if (!canRunProxyConnect()) configFactory().addServerToL1Config(null, dsoPort, adminPort);
      serverControl = helper.getServerControl();
    } else if (isActivePassive() && canRunActivePassive()) {
      setUpActivePassiveServers(portChooser, jvmArgs);
    } else {
      int dsoPort = portChooser.chooseRandomPort();
      int adminPort = portChooser.chooseRandomPort();
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
      if (!canRunProxyConnect()) configFactory().addServerToL1Config(null, dsoPort, -1);
    }

    if (canRunProxyConnect()) {
      setupProxyConnect(helper, portChooser);
    }

    this.doSetUp(this);

    if (isCrashy() && canRunCrash()) {
      crashTestState = new TestState(false);
      crasher = new ServerCrasher(serverControl, helper.getServerCrasherConfig().getRestartInterval(), helper
          .getServerCrasherConfig().isCrashy(), crashTestState);
      if (canRunProxyConnect()) crasher.setProxyConnectMode(true);
      crasher.startAutocrash();
    }
  }

  private final void setUpActivePassiveServers(PortChooser portChooser, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    setJavaHome();
    apSetupManager = new ActivePassiveTestSetupManager();
    setupActivePassiveTest(apSetupManager);
    apServerManager = new ActivePassiveServerManager(mode()
        .equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE), getTempDirectory(), portChooser,
                                                     ActivePassiveServerConfigCreator.DEV_MODE, apSetupManager,
                                                     runnerConfig.startTimeout(), javaHome, configFactory(), jvmArgs);
    apServerManager.addServersToL1Config(configFactory);
  }

  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    throw new AssertionError("The sub-class (test) should override this method.");
  }

  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    // do nothing
  }

  private final void setupProxyConnect(RestartTestHelper helper, PortChooser portChooser) throws Exception {
    int dsoPort = 0;
    int jmxPort = 0;

    if (helper != null) {
      dsoPort = helper.getServerPort();
      jmxPort = helper.getAdminPort();
      // for crash+proxy, set crash interval to 60 sec
      helper.getServerCrasherConfig().setRestartInterval(60 * 1000);
    } else if (isActivePassive() && canRunActivePassive()) {
      // not doing active-passive for proxy yet
      throw new AssertionError("Proxy-connect is yet not running with active-passive mode");
    } else {
      dsoPort = portChooser.chooseRandomPort();
      jmxPort = portChooser.chooseRandomPort();
    }

    int dsoProxyPort = portChooser.chooseRandomPort();
    ProxyConnectManagerImpl mgr = ProxyConnectManagerImpl.getManager();
    mgr.setDsoPort(dsoPort);
    mgr.setProxyPort(dsoProxyPort);
    mgr.setupProxy();
    setupProxyConnectTest(mgr);

    ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(dsoPort);
    ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(jmxPort);
    configFactory().addServerToL1Config(null, dsoProxyPort, -1);
  }

  protected void setupProxyConnectTest(ProxyConnectManagerImpl mgr) {
    /*
     * subclass can overwrite to change the test parameters.
     */
    mgr.setProxyWaitTime(20 * 1000);
    mgr.setProxyDownTime(100);
  }

  protected boolean useExternalProcess() {
    return getTestConfigObject().isL2StartupModeExternal();
  }

  // only used by regular system tests (not crash or active-passive)
  protected final void setUpControlledServer(TestTVSConfigurationSetupManagerFactory factory,
                                             DSOClientConfigHelper helper, int serverPort, int adminPort,
                                             String configFile) throws Exception {
    setUpControlledServer(factory, helper, serverPort, adminPort, configFile, new ArrayList());
  }

  // used by ResolveTwoActiveServersTest... only works with 2 servers !!
  protected final void setUpForMultipleExternalProcesses(TestTVSConfigurationSetupManagerFactory factory,
                                                         DSOClientConfigHelper helper, int[] dsoPorts, int[] jmxPorts,
                                                         int[] l2GroupPorts, int[] proxyPorts, String[] serverNames,
                                                         File[] configFiles) throws Exception {
    assertEquals(dsoPorts.length, 2);

    controlledCrashMode = true;
    setJavaHome();
    serverControls = new ServerControl[dsoPorts.length];

    proxies = new TCPProxy[2];

    for (int i = 0; i < 2; i++) {
      proxies[i] = new TCPProxy(proxyPorts[i], InetAddress.getLocalHost(), l2GroupPorts[i], 0L, false, new File("."));
      proxies[i].setReuseAddress(true);
      serverControls[i] = new ExtraProcessServerControl("localhost", dsoPorts[i], jmxPorts[i], configFiles[i]
          .getAbsolutePath(), true, serverNames[i], null, javaHome, true);
    }
    setUp(factory, helper, true);
  }

  protected final void setUpControlledServer(TestTVSConfigurationSetupManagerFactory factory,
                                             DSOClientConfigHelper helper, int serverPort, int adminPort,
                                             String configFile, List jvmArgs) throws Exception {

    controlledCrashMode = true;
    setUpExternalProcess(factory, helper, serverPort, adminPort, configFile, jvmArgs);
  }

  protected void setUpExternalProcess(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper,
                                      int serverPort, int adminPort, String configFile, List jvmArgs) throws Exception {
    setJavaHome();
    assertNotNull(jvmArgs);
    serverControl = new ExtraProcessServerControl("localhost", serverPort, adminPort, configFile, true, javaHome,
                                                  jvmArgs);
    setUp(factory, helper);

    ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(serverPort);
    ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
    configFactory().addServerToL1Config(null, serverPort, adminPort);
  }

  private final void setUp(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper)
      throws Exception {
    setUp(factory, helper, false);
  }

  private final void setUp(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper,
                           boolean serverControlsSet) throws Exception {
    super.setUp();
    this.configFactory = factory;
    this.configHelper = helper;
    if (serverControlsSet) {
      transparentAppConfig = new TransparentAppConfig(getApplicationClass().getName(), new TestGlobalIdGenerator(),
                                                      DEFAULT_CLIENT_COUNT, DEFAULT_INTENSITY, serverControls, proxies);
    } else {
      transparentAppConfig = new TransparentAppConfig(getApplicationClass().getName(), new TestGlobalIdGenerator(),
                                                      DEFAULT_CLIENT_COUNT, DEFAULT_INTENSITY, serverControl,
                                                      DEFAULT_VALIDATOR_COUNT, DEFAULT_ADAPTED_MUTATOR_COUNT,
                                                      DEFAULT_ADAPTED_VALIDATOR_COUNT);
    }
  }

  protected synchronized final String mode() {
    if (mode == null) {
      mode = getTestConfigObject().transparentTestsMode();
    }

    return mode;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    // Nothing here, by default
  }

  private boolean isCrashy() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH.equals(mode());
  }

  private boolean isActivePassive() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE.equals(mode());
  }

  public DSOClientConfigHelper getConfigHelper() {
    return this.configHelper;
  }

  public TVSConfigurationSetupManagerFactory getConfigFactory() {
    return configFactory;
  }

  public DistributedTestRunnerConfig getRunnerConfig() {
    return this.runnerConfig;
  }

  public void setApplicationConfigBuilder(ApplicationConfigBuilder builder) {
    this.possibleApplicationConfigBuilder = builder;
  }

  public TransparentAppConfig getTransparentAppConfig() {
    return this.transparentAppConfig;
  }

  protected ApplicationConfigBuilder getApplicationConfigBuilder() {
    if (possibleApplicationConfigBuilder != null) return possibleApplicationConfigBuilder;
    else return transparentAppConfig;
  }

  protected abstract Class getApplicationClass();

  protected Map getOptionalAttributes() {
    return new HashMap();
  }

  String getServerPortProp() {
    return System.getProperty("test.base.server.port");
  }

  private boolean getStartServer() {
    return getServerPortProp() == null && mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL)
           && !controlledCrashMode && !useExternalProcess();
  }

  public void initializeTestRunner() throws Exception {
    initializeTestRunner(false);
  }

  public void initializeTestRunner(boolean isMutateValidateTest) throws Exception {
    this.runner = new DistributedTestRunner(runnerConfig, configFactory, configHelper, getApplicationClass(),
                                            getOptionalAttributes(), getApplicationConfigBuilder()
                                                .newApplicationConfig(), getStartServer(), isMutateValidateTest,
                                            (isActivePassive() && canRunActivePassive()), apServerManager,
                                            transparentAppConfig);
  }

  protected boolean canRun() {
    return (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL) && canRunNormal())
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH) && canRunCrash())
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE) && canRunActivePassive());
  }

  protected boolean canRunNormal() {
    return true;
  }

  protected boolean canRunCrash() {
    return false;
  }

  protected boolean canRunActivePassive() {
    return false;
  }

  protected boolean canRunProxyConnect() {
    return false;
  }

  protected boolean enableL1Reconnect() {
    return false;
  }

  protected void startServerControlsAndProxies() throws Exception {
    assertEquals(serverControls.length, 2);
    for (int i = 0; i < serverControls.length; i++) {
      serverControls[i].start(30 * 1000);

      // make sure that the first server becomes active
      if (i == 0) {
        Thread.sleep(10 * 1000);
      } else {
        proxies[1].start();
        proxies[0].start();
      }
    }
  }

  public void test() throws Exception {
    if (canRun()) {
      if (canRunProxyConnect()) {
        ProxyConnectManagerImpl.getManager().proxyUp();
        ProxyConnectManagerImpl.getManager().startProxyTest();
      }
      if (controlledCrashMode && isActivePassive() && apServerManager != null) {
        apServerManager.startServers();
      } else if (controlledCrashMode && serverControl != null) {
        serverControl.start(30 * 1000);
      } else if (controlledCrashMode && serverControls != null && proxies != null) {
        startServerControlsAndProxies();
      } else if (useExternalProcess()) {
        serverControl.start(30 * 1000);
      }
      this.runner.run();

      if (this.runner.executionTimedOut() || this.runner.startTimedOut()) {
        try {
          System.err.println("##### About to shutdown server crasher");
          synchronized (crashTestState) {
            crashTestState.setTestState(TestState.STOPPING);
          }
          System.err.println("##### About to dump server");
          dumpServers();
        } finally {
          if (pid != 0) {
            System.out.println("Thread dumping test process");
            ThreadDump.dumpThreadsMany(getThreadDumpCount(), getThreadDumpInterval());
          }
        }
      }

      if (!this.runner.success()) {
        AssertionFailedError e = new AssertionFailedError(new ErrorContextFormatter(this.runner.getErrors())
            .formatForExceptionMessage());
        throw e;
      }
    } else {
      System.err.println("NOTE: " + getClass().getName() + " can't be run in mode '" + mode()
                         + "', and thus will be skipped.");
    }
  }

  private void dumpServers() throws Exception {
    if (serverControl != null && serverControl.isRunning()) {
      System.out.println("Dumping server=[" + serverControl.getDsoPort() + "]");
      dumpServerControl(serverControl);
    }

    if (apServerManager != null) {
      apServerManager.dumpAllServers(pid, getThreadDumpCount(), getThreadDumpInterval());
      pid = apServerManager.getPid();
    }

    if (serverControls != null) {
      for (int i = 0; i < serverControls.length; i++) {
        dumpServerControl(serverControls[i]);
      }
    }

    if (runner != null) {
      runner.dumpServer();
    } else {
      System.err.println("Runner is null !!");
    }
  }

  private void dumpServerControl(ServerControl control) throws Exception {
    JMXConnector jmxConnector = ActivePassiveServerManager.getJMXConnector(control.getAdminPort());
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    L2DumperMBean mbean = (L2DumperMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                                        L2DumperMBean.class, true);
    while (true) {
      try {
        mbean.doServerDump();
        break;
      } catch (Exception e) {
        System.out.println("Could not find L2DumperMBean... sleep for 1 sec.");
        Thread.sleep(1000);
      }
    }

    if (pid != 0) {
      mbean.setThreadDumpCount(getThreadDumpCount());
      mbean.setThreadDumpInterval(getThreadDumpInterval());
      System.out.println("Thread dumping server=[" + serverControl.getDsoPort() + "] pid=[" + pid + "]");
      pid = mbean.doThreadDump();
    }
    jmxConnector.close();
  }

  protected void tearDown() throws Exception {
    if (controlledCrashMode) {
      if (isActivePassive() && canRunActivePassive()) {
        System.out.println("Currently running java processes: " + ProcessInfo.ps_grep_java());
        apServerManager.stopAllServers();
      } else if (isCrashy() && canRunCrash()) {
        synchronized (crashTestState) {
          crashTestState.setTestState(TestState.STOPPING);
          if (serverControl.isRunning()) {
            serverControl.shutdown();
          }
        }
      }
    }

    if (serverControls != null) {
      for (int i = 0; i < serverControls.length; i++) {
        if (serverControls[i].isRunning()) {
          serverControls[i].shutdown();
        }
      }
    }

    if (serverControl != null && serverControl.isRunning()) {
      serverControl.shutdown();
    }

    super.tearDown();
  }

  protected void doDumpServerDetails() {
    try {
      dumpServers();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static final class ErrorContextFormatter {
    private final Collection   contexts;
    private final StringBuffer buf = new StringBuffer();

    public ErrorContextFormatter(Collection contexts) {
      this.contexts = contexts;
    }

    private void div() {
      buf.append("\n**************************************************************\n");
    }

    private void println(Object message) {
      buf.append(message + "\n");
    }

    public String formatForExceptionMessage() {
      buf.delete(0, buf.length());
      div();
      println("There are " + contexts.size() + " error contexts:");
      int count = 1;
      for (Iterator i = contexts.iterator(); i.hasNext();) {
        ErrorContext ctxt = (ErrorContext) i.next();
        println("Error context " + count + "\n");
        println(ctxt);
        count++;
      }
      println("End error contexts.");
      div();
      return buf.toString();
    }
  }

  protected File writeMinimalConfig(int port, int adminPort) {
    TerracottaConfigBuilder builder = createConfigBuilder(port, adminPort);
    FileOutputStream out = null;
    File configFile = null;
    try {
      configFile = getTempFile("config-file.xml");
      out = new FileOutputStream(configFile);
      CopyUtils.copy(builder.toString(), out);
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    } finally {
      try {
        out.close();
      } catch (Exception e) { /* oh well, we tried */
      }
    }

    return configFile;
  }

  protected TerracottaConfigBuilder createConfigBuilder(int port, int adminPort) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);

    return out;
  }

  /*
   * State inner class
   */

}

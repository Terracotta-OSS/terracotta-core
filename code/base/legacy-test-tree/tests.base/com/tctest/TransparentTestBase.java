/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerConfigCreator;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.test.restart.ServerCrasher;
import com.tc.util.PortChooser;
import com.tc.util.runtime.ThreadDump;
import com.tctest.runner.DistributedTestRunner;
import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.TestGlobalIdGenerator;
import com.tctest.runner.TransparentAppConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.AssertionFailedError;

public abstract class TransparentTestBase extends BaseDSOTestCase implements TransparentTestIface, TestConfigurator {

  public static final int                         DEFAULT_CLIENT_COUNT    = 2;
  public static final int                         DEFAULT_INTENSITY       = 10;
  public static final int                         DEFAULT_VALIDATOR_COUNT = 0;

  private TestTVSConfigurationSetupManagerFactory configFactory;
  private DSOClientConfigHelper                   configHelper;
  protected DistributedTestRunner                 runner;
  private DistributedTestRunnerConfig             runnerConfig            = new DistributedTestRunnerConfig();
  private TransparentAppConfig                    transparentAppConfig;
  private ApplicationConfigBuilder                possibleApplicationConfigBuilder;

  private String                                  mode;
  private ServerControl                           serverControl;
  private boolean                                 controlledCrashMode     = false;
  private ServerCrasher                           crasher;

  // for active-passive tests
  private ActivePassiveServerManager              apServerManager;
  private ActivePassiveTestSetupManager           apSetupManager;

  protected void setUp() throws Exception {
    setUp(configFactory(), configHelper());

    RestartTestHelper helper = null;

    if (isCrashy() && canRunCrash()) {
      helper = new RestartTestHelper(mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH),
                                     new RestartTestEnvironment(getTempDirectory(), new PortChooser(),
                                                                RestartTestEnvironment.PROD_MODE));
      // ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(helper.getServerPort());
      // configFactory().activateConfigurationChange();
      configFactory().addServerToL1Config(null, helper.getServerPort(), -1);
      configFactory().addServerToL2Config(null, helper.getServerPort(), helper.getAdminPort());

      serverControl = helper.getServerControl();
    } else if (isActivePassive() && canRunActivePassive()) {
      setUpActivePassiveServers();
    } else {
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(0);
    }

    this.doSetUp(this);

    if (isCrashy() && canRunCrash()) {
      crasher = new ServerCrasher(serverControl, helper.getServerCrasherConfig().getRestartInterval(), helper
          .getServerCrasherConfig().isCrashy());
      crasher.startAutocrash();
    }
  }

  private final void setUpActivePassiveServers() throws Exception {
    controlledCrashMode = true;
    apSetupManager = new ActivePassiveTestSetupManager();
    setupActivePassiveTest(apSetupManager);
    apServerManager = new ActivePassiveServerManager(mode()
        .equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE), getTempDirectory(), new PortChooser(),
                                                     ActivePassiveServerConfigCreator.DEV_MODE, apSetupManager,
                                                     runnerConfig.startTimeout());
    apServerManager.addServersToL1Config(configFactory);
  }

  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    throw new AssertionError("The sub-class (test) should override this method.");
  }

  protected final void setUpControlledServer(TestTVSConfigurationSetupManagerFactory factory,
                                             DSOClientConfigHelper helper, int serverPort, int adminPort,
                                             String configFile) throws Exception {
    controlledCrashMode = true;
    serverControl = new ExtraProcessServerControl("localhost", serverPort, adminPort, configFile, true);
    setUp(factory, helper);

    configFactory().addServerToL1Config(null, serverPort, adminPort);
    configFactory().addServerToL2Config(null, serverPort, adminPort);
  }

  private final void setUp(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper)
      throws Exception {
    super.setUp();
    this.configFactory = factory;
    this.configHelper = helper;
    transparentAppConfig = new TransparentAppConfig(getApplicationClass().getName(), new TestGlobalIdGenerator(),
                                                    DEFAULT_CLIENT_COUNT, DEFAULT_INTENSITY, serverControl,
                                                    DEFAULT_VALIDATOR_COUNT);
  }

  protected synchronized final String mode() {
    if (mode == null) {
      try {
        mode = TestConfigObject.getInstance().transparentTestsMode();
      } catch (IOException ioe) {
        throw new RuntimeException("Can't get mode", ioe);
      }
    }

    return mode;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    // Nothing here, by default
  }

  private boolean isCrashy() {
    return mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH);
  }

  private boolean isActivePassive() {
    return mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE);
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

  public int getServerPort() {
    if (getStartServer()) return this.runner.getServerPort();
    else return new Integer(getServerPortProp()).intValue();
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
           && !controlledCrashMode;
  }

  public void initializeTestRunner() throws Exception {
    initializeTestRunner(false);
  }

  public void initializeTestRunner(boolean isMutateValidateTest) throws Exception {
    this.runner = new DistributedTestRunner(runnerConfig, configFactory, configHelper, getApplicationClass(),
                                            getOptionalAttributes(), getApplicationConfigBuilder()
                                                .newApplicationConfig(), transparentAppConfig.getClientCount(),
                                            transparentAppConfig.getApplicationInstancePerClientCount(),
                                            getStartServer(), isMutateValidateTest, transparentAppConfig
                                                .getValidatorCount(), (isActivePassive() && canRunActivePassive()),
                                            apServerManager);
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

  public void test() throws Exception {
    if (canRun()) {
      if (controlledCrashMode && !isActivePassive()) {
        serverControl.start(30 * 1000);
      } else if (controlledCrashMode) {
        apServerManager.startServers();
      }
      this.runner.run();

      if (this.runner.executionTimedOut() || this.runner.startTimedOut()) {
        try {
          this.runner.dumpServer();
        } finally {
          ThreadDump.dumpThreadsMany(3, 1000L);
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

  protected void tearDown() throws Exception {
    if (controlledCrashMode) {
      if (isActivePassive() && canRunActivePassive()) {
        apServerManager.stopAllServers();
        apServerManager = null;
      } else if (isCrashy() && canRunCrash()) {
        crasher.stop();
      }
    }
    super.tearDown();
  }

  protected void doDumpServerDetails() {
    try {
      if (this.runner != null) {
        this.runner.dumpServer();
      } else {
        System.err.println("Runner is null !!");
      }
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

}

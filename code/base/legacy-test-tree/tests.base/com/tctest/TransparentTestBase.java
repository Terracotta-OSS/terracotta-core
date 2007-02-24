/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.test.TestConfigObject;
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
import java.util.Iterator;

import junit.framework.AssertionFailedError;

public abstract class TransparentTestBase extends BaseDSOTestCase implements TransparentTestIface, TestConfigurator {

  public static final int                     DEFAULT_CLIENT_COUNT = 2;
  public static final int                     DEFAULT_INTENSITY    = 10;

  private TVSConfigurationSetupManagerFactory configFactory;
  private DSOClientConfigHelper               configHelper;
  protected DistributedTestRunner             runner;
  private DistributedTestRunnerConfig         runnerConfig         = new DistributedTestRunnerConfig();
  private TransparentAppConfig                transparentAppConfig;
  private ApplicationConfigBuilder            possibleApplicationConfigBuilder;

  private String                              mode;
  private ServerControl                       serverControl;
  private boolean                             controlledCrashMode  = false;
  private ServerCrasher                       crasher;

  protected void setUp() throws Exception {
    setUp(configFactory(), configHelper());

    RestartTestHelper helper = null;

    if (isCrashy()) {
      helper = new RestartTestHelper(mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH),
                                     new RestartTestEnvironment(getTempDirectory(), new PortChooser(),
                                                                RestartTestEnvironment.PROD_MODE));
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(helper.getServerPort());
      serverControl = helper.getServerControl();
    } else {
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(0);
    }

    this.doSetUp(this);

    if (isCrashy()) {
      crasher = new ServerCrasher(serverControl, helper.getServerCrasherConfig().getRestartInterval(), helper
          .getServerCrasherConfig().isCrashy());
      crasher.startAutocrash();
    }
  }

  protected final void setUpControlledServer(TVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper,
                                             int serverPort, int adminPort, String configFile) throws Exception {
    controlledCrashMode = true;
    serverControl = new ExtraProcessServerControl("localhost", serverPort, adminPort, configFile, true);
    setUp(factory, helper);
  }

  private final void setUp(TVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper) throws Exception {
    super.setUp();
    this.configFactory = factory;
    this.configHelper = helper;
    transparentAppConfig = new TransparentAppConfig(getApplicationClass().getName(), new TestGlobalIdGenerator(),
                                                    DEFAULT_CLIENT_COUNT, DEFAULT_INTENSITY, serverControl);
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
    return mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_RESTART)
           || mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH);
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

  String getServerPortProp() {
    return System.getProperty("test.base.server.port");
  }

  protected boolean getStartServer() {
    return getServerPortProp() == null && mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL)
           && !controlledCrashMode;
  }

  public void initializeTestRunner() throws Exception {
    this.runner = new DistributedTestRunner(this.runnerConfig, configFactory, this.configHelper, getApplicationClass(),
                                            getApplicationConfigBuilder().newApplicationConfig(),
                                            this.transparentAppConfig.getClientCount(), this.transparentAppConfig
                                                .getApplicationInstancePerClientCount(), getStartServer());
  }

  protected boolean canRun() {
    return (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL) && canRunNormal())
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH) && canRunCrash())
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_RESTART) && canRunRestart());
  }

  protected boolean canRunNormal() {
    return true;
  }

  protected boolean canRunCrash() {
    return false;
  }

  protected boolean canRunRestart() {
    return false;
  }

  public void test() throws Exception {
    if (canRun()) {
      if (controlledCrashMode) serverControl.start(30 * 1000);
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

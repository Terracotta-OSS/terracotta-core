package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class L1ReconnectEnabledTestApp extends AbstractTransparentApp {

  public static final String CONFIG_FILE = "config-file";
  public static final String PORT_NUMBER = "port-number";
  public static final String HOST_NAME   = "host-name";
  public static final String JMX_PORT  = "jmx-port";

  private ApplicationConfig  appCfg;

  public L1ReconnectEnabledTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appCfg = cfg;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = L1ReconnectEnabledTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addIncludePattern(testClass + "$*");
  }

  public void run() {
    testL1ReconnectConfig();
  }

  private void testL1ReconnectConfig() {
    TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                  TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                  null,
                                                                                                  new FatalIllegalConfigurationChangeHandler());
    int portNumber = Integer.parseInt(appCfg.getAttribute(PORT_NUMBER));
    int jmxPort = Integer.parseInt(appCfg.getAttribute(JMX_PORT));
    ((SettableConfigItem) factory.l2DSOConfig().listenPort()).setValue(portNumber);
    ((SettableConfigItem) factory.l2CommonConfig().jmxPort()).setValue(jmxPort);
    factory.addServerToL1Config(null, portNumber, jmxPort);
    try {
      DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(factory
          .createL1TVSConfigurationSetupManager());
      ReconnectConfig l1ReconnectConfig = configHelper.getL1ReconnectProperties();

      // verify
      Assert.eval(l1ReconnectConfig.getReconnectEnabled() == true);
      Assert.eval(l1ReconnectConfig.getReconnectTimeout() == L1ReconnectEnabledTest.L1_RECONNECT_TIMEOUT);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
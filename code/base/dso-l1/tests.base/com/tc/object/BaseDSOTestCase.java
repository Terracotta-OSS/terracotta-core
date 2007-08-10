/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.test.TCTestCase;
import com.terracottatech.config.AdditionalBootJarClasses;

import java.io.IOException;

/**
 * The base of all DSO tests that use config.
 */
public class BaseDSOTestCase extends TCTestCase {

  private Exception failTestException;

  private class TestFailingIllegalConfigChangeHandler implements IllegalConfigurationChangeHandler {
    public void changeFailed(ConfigItem item, Object oldValue, Object newValue) {
      failTestException = new Exception("An attempt was made to illegally change the config item " + item + " from "
                                        + ArrayUtils.toString(oldValue) + " to " + ArrayUtils.toString(newValue));
    }
  }

  public void runBare() throws Throwable {
    super.runBare();
    if (this.failTestException != null) throw this.failTestException;
  }

  private TestTVSConfigurationSetupManagerFactory factory;
  private L1TVSConfigurationSetupManager          l1Manager;
  private DSOClientConfigHelper                   configHelper;

  protected final synchronized TestTVSConfigurationSetupManagerFactory configFactory()
      throws ConfigurationSetupException {
    if (this.factory == null) this.factory = createDistributedConfigFactory();
    return this.factory;
  }

  protected final TestTVSConfigurationSetupManagerFactory createDistributedConfigFactory()
      throws ConfigurationSetupException {
    TestTVSConfigurationSetupManagerFactory out;
    out = new TestTVSConfigurationSetupManagerFactory(TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                      null, new TestFailingIllegalConfigChangeHandler());

    prepareFactory(out);
    return out;
  }

  private void prepareFactory(TestTVSConfigurationSetupManagerFactory out) throws ConfigurationSetupException {
    // We add a root to make sure there's at least *some* application config. Otherwise, the config system will wait for
    // it on system startup.
    /*
     * Roots roots = Roots.Factory.newInstance(); Root dummyRoot = roots.addNewRoot();
     * dummyRoot.setFieldName("com.dummy.whatever.Bar.x");
     */

    AdditionalBootJarClasses classes = AdditionalBootJarClasses.Factory.newInstance();
    classes.setIncludeArray(new String[] { "com.dummy.whatever.Bar" });

    ((SettableConfigItem) out.dsoApplicationConfig().additionalBootJarClasses()).setValue(classes);
    // ((SettableConfigItem) out.dsoApplicationConfig().roots()).setValue(roots);

    try {
      ((SettableConfigItem) out.l2CommonConfig().dataPath()).setValue(getTempFile("l2-data").toString());
      ((SettableConfigItem) out.l2CommonConfig().logsPath()).setValue(getTempFile("l2-logs").toString());
      ((SettableConfigItem) out.l1CommonConfig().logsPath()).setValue(getTempFile("l1-logs").toString());
    } catch (IOException ioe) {
      throw new ConfigurationSetupException("Can't set up log and data paths", ioe);
    }

    out.activateConfigurationChange();
  }

  protected final TestTVSConfigurationSetupManagerFactory createCentralizedConfigFactory()
      throws ConfigurationSetupException {
    TestTVSConfigurationSetupManagerFactory out;
    out = new TestTVSConfigurationSetupManagerFactory(new TestFailingIllegalConfigChangeHandler());

    prepareFactory(out);
    return out;
  }

  protected final synchronized L1TVSConfigurationSetupManager l1Manager() throws ConfigurationSetupException {
    if (this.l1Manager == null) this.l1Manager = createL1ConfigManager();
    return this.l1Manager;
  }

  protected final L1TVSConfigurationSetupManager createL1ConfigManager() throws ConfigurationSetupException {
    return configFactory().createL1TVSConfigurationSetupManager();
  }

  protected final synchronized DSOClientConfigHelper configHelper() throws ConfigurationSetupException {
    if (this.configHelper == null) this.configHelper = createClientConfigHelper();
    return this.configHelper;
  }

  protected final DSOClientConfigHelper createClientConfigHelper() throws ConfigurationSetupException {
    return new StandardDSOClientConfigHelperImpl(true, createL1ConfigManager());
  }

  // TODO: fix this
  protected final void makeClientUsePort(int whichPort) throws ConfigurationSetupException {
    ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(whichPort);
  }

  public BaseDSOTestCase() {
    super();
  }

  public BaseDSOTestCase(String arg0) {
    super(arg0);
  }

  protected void tearDown() throws Exception {
    this.factory = null;
    this.configHelper = null;
    this.l1Manager = null;
  }
}

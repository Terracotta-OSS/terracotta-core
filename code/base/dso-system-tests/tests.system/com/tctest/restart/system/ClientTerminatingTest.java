/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import org.apache.commons.io.CopyUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public class ClientTerminatingTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 2;

  private File             configFile;
  private int              port;

  private int              adminPort;

  protected Class getApplicationClass() {
    return ClientTerminatingTestApp.class;
  }

  public void setUp() throws Exception {
    // XXX: ERR! HACK! Will collide eventually
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    configFile = getTempFile("config-file.xml");
    writeConfigFile();
    StandardTVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(
                                                                                                          new String[] {
                                                                                                              StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD,
                                                                                                              configFile
                                                                                                                  .getAbsolutePath() },
                                                                                                          true,
                                                                                                          new FatalIllegalConfigurationChangeHandler());

    L1TVSConfigurationSetupManager manager = factory.createL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelper(manager), port, adminPort, configFile
        .getAbsolutePath());
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();

    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(ClientTerminatingTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(ClientTerminatingTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(ClientTerminatingTestApp.HOST_NAME, "localhost");
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = ClientTerminatingTestApp.createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      CopyUtils.copy(builder.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

}

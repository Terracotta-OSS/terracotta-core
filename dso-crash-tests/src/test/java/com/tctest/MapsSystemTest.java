/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class MapsSystemTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;
  private File             configFile;

  @Override
  protected Class getApplicationClass() {
    return MapsSystemTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    int port = pc.chooseRandomPort();
    int jmxPort = pc.chooseRandomPort();
    int groupPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");

    writeConfigFile(port, jmxPort);

    ArrayList jvmArgs = new ArrayList();
    setJvmArgsL1Reconnect(jvmArgs);
    setUpControlledServer(configFactory(), configHelper(), port, jmxPort, groupPort, configFile.getAbsolutePath(),
                          jvmArgs);
    doSetUp(this);
  }

  private synchronized void writeConfigFile(int port, int jmxPort) {
    try {
      TerracottaConfigBuilder builder = createConfig(port, jmxPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    return out;
  }
}
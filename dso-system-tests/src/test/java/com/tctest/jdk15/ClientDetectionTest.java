/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public class ClientDetectionTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;
  private int              port;
  private File             configFile;
  private int              adminPort;

  public ClientDetectionTest() {
    timebombTestForRewrite();
  }

  @Override
  protected Class getApplicationClass() {
    return ClientDetectionTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(ClientDetectionTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(ClientDetectionTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(ClientDetectionTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(ClientDetectionTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    int groupPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    writeConfigFile();

    setUpControlledServer(configFactory(), configHelper(), port, adminPort, groupPort, configFile.getAbsolutePath());
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.write(builder.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort) {
    String testClassName = ClientDetectionTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();

    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    out.getApplication().getDSO()
        .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2 });

    RootConfigBuilder testApp_barrier4 = new RootConfigBuilderImpl(ClientDetectionTestApp.class, "barrier4", "barrier4");

    RootConfigBuilder L1_barrier4 = new RootConfigBuilderImpl(ClientDetectionTestApp.L1Client.class, "barrier4",
                                                              "barrier4");

    out.getApplication().getDSO().setRoots(new RootConfigBuilder[] { testApp_barrier4, L1_barrier4 });

    return out;
  }
}

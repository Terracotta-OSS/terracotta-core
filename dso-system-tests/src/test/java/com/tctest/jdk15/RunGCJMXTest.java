/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.RunGCJMXTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;

public class RunGCJMXTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;
  private int              port;
  private File             configFile;
  private int              adminPort;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(LockStatisticsJMXTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(LockStatisticsJMXTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(LockStatisticsJMXTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(LockStatisticsJMXTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  @Override
  protected Class getApplicationClass() {
    return RunGCJMXTestApp.class;
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    int groupPort = pc.chooseRandomPort();
    configFile = getTempFile("config-file.xml");
    writeConfigFile();

    setUpControlledServer(configFactory(), configHelper(), port, adminPort, groupPort, configFile.getAbsolutePath());
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      StringReader reader = new StringReader(builder.toString());
      IOUtils.copy(reader, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort) {
    String testClassName = LockStatisticsJMXTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();

    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY);

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    out.getApplication().getDSO().setInstrumentedClasses(
                                                         new InstrumentedClassConfigBuilder[] { instrumented1,
                                                             instrumented2 });

    return out;
  }
}
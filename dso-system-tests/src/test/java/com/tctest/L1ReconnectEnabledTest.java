/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class L1ReconnectEnabledTest extends TransparentTestBase {
  private static final int NODE_COUNT           = 1;
  public static final int  L1_RECONNECT_TIMEOUT = 1234;
  private int              port;
  private File             configFile;
  private int              jmxPort;
  private int              groupPort;

  @Override
  protected Class getApplicationClass() {
    return L1ReconnectEnabledTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(L1ReconnectEnabledTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(L1ReconnectEnabledTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(L1ReconnectEnabledTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(L1ReconnectEnabledTestApp.JMX_PORT, String.valueOf(jmxPort));
  }

  @Override
  protected void setJvmArgsL1Reconnect(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();

    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED + "=true");

    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "" + L1_RECONNECT_TIMEOUT);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "" + L1_RECONNECT_TIMEOUT);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS + "=" + L1_RECONNECT_TIMEOUT);
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    jmxPort = pc.chooseRandomPort();
    groupPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    writeConfigFile();

    ArrayList jvmArgs = new ArrayList();
    setJvmArgsL1Reconnect(jvmArgs);
    setUpControlledServer(configFactory(), configHelper(), port, jmxPort, groupPort, configFile.getAbsolutePath(),
                          jvmArgs);
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, jmxPort, groupPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort, int groupPort) {
    String testClassName = L1ReconnectEnabledTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();

    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setL2GroupPort(groupPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    out.getApplication().getDSO()
        .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2 });

    return out;
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.TcProperty;
import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TcPropertiesBuilder;
import com.tc.config.schema.test.TcPropertyBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.properties.TCPropertiesConsts;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public class TcPropertiesOverWriteTest extends TransparentTestBase {
  private static final int NODE_COUNT                                     = 1;
  private int              port;
  private File             configFile;
  private int              jmxPort;
  private static final int NUMBER_OF_TC_PROPERTIES                        = 4;
  private TcProperty[]     propertiesToTest;
  public static String     L1_CACHEMANAGER_ENABLED_VALUE                  = "true";
  public static String     L1_LOGGING_MAX_LOGFILE_SIZE_VALUE              = "1234";
  public static String     L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES_VALUE = "5678";
  public static String     L1_CACHEMANAGER_LEASTCOUNT_VALUE               = "15";

  @Override
  protected Class getApplicationClass() {
    return TcPropertiesOverWriteTestApp.class;
  }

  protected boolean enableSetTcProperties() {
    return true;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(TcPropertiesOverWriteTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(TcPropertiesOverWriteTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(TcPropertiesOverWriteTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(TcPropertiesOverWriteTestApp.JMX_PORT, String.valueOf(jmxPort));
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    jmxPort = pc.chooseRandomPort();
    int groupPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    // set the properties to be overwritten, these properties would be overwridden by the tc-config
    propertiesToTest = new TcProperty[NUMBER_OF_TC_PROPERTIES];
    propertiesToTest[0] = new TcProperty(TCPropertiesConsts.L1_CACHEMANAGER_ENABLED, L1_CACHEMANAGER_ENABLED_VALUE);
    propertiesToTest[1] = new TcProperty(TCPropertiesConsts.LOGGING_MAX_LOGFILE_SIZE, L1_LOGGING_MAX_LOGFILE_SIZE_VALUE);

    // this property is also given as a system property which has higher precedence to tc-config
    // this would not get overridden
    propertiesToTest[2] = new TcProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES, "2345");

    // this property is also given by tc.properties file which has higher precedence to tc-config
    // this would not get overridden
    propertiesToTest[3] = new TcProperty(TCPropertiesConsts.L1_CACHEMANAGER_LEASTCOUNT, "9000");
    writeConfigFile();

    setUpControlledServer(configFactory(), configHelper(), port, jmxPort, groupPort, configFile.getAbsolutePath(), null);
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, jmxPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private TerracottaConfigBuilder createConfig(int dsoPort, int adminPort) {
    TerracottaConfigBuilder tcConfigBuilder = new TerracottaConfigBuilder();
    TcPropertyBuilder[] tcPropertyBuilder = new TcPropertyBuilder[NUMBER_OF_TC_PROPERTIES];

    for (int i = 0; i < NUMBER_OF_TC_PROPERTIES; i++) {
      tcPropertyBuilder[i] = new TcPropertyBuilder(propertiesToTest[i].getPropertyName(), propertiesToTest[i]
          .getPropertyValue());
    }

    TcPropertiesBuilder tcPropertiesBuilder = new TcPropertiesBuilder();
    tcPropertiesBuilder.setTcProperties(tcPropertyBuilder);

    tcConfigBuilder.setTcProperties(tcPropertiesBuilder);

    tcConfigBuilder.getServers().getL2s()[0].setDSOPort(port);
    tcConfigBuilder.getServers().getL2s()[0].setJMXPort(adminPort);
    tcConfigBuilder.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    String testClassName = TcPropertiesOverWriteTest.class.getName();
    String testClassAppName = TcPropertiesOverWriteTestApp.class.getName();

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassAppName + "*");

    tcConfigBuilder.getApplication().getDSO().setInstrumentedClasses(
                                                                     new InstrumentedClassConfigBuilder[] {
                                                                         instrumented1, instrumented2 });

    RootConfigBuilder obj = new RootConfigBuilderImpl(TcPropertiesOverWriteTestApp.class, "obj", "obj");
    tcConfigBuilder.getApplication().getDSO().setRoots(new RootConfigBuilder[] { obj });

    LockConfigBuilder lock = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock.setMethodExpression("* " + testClassAppName + "*.*(..)");
    lock.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    tcConfigBuilder.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock });

    return tcConfigBuilder;
  }
}

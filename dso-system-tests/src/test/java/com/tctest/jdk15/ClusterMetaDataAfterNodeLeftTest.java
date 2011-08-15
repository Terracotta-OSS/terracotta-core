/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.ClusterEventsL1Client;
import com.tctest.ClusterEventsTestApp;
import com.tctest.ClusterEventsTestListener;
import com.tctest.ClusterEventsTestState;
import com.tctest.ClusterMetaDataAfterNodeLeftTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public class ClusterMetaDataAfterNodeLeftTest extends TransparentTestBase {

  private int  port;
  private File configFile;
  private int  adminPort;

  @Override
  protected Class getApplicationClass() {
    return ClusterMetaDataAfterNodeLeftTestApp.class;
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

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(ClusterEventsTestApp.NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(ClusterMetaDataAfterNodeLeftTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(ClusterMetaDataAfterNodeLeftTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(ClusterMetaDataAfterNodeLeftTestApp.HOST_NAME, "localhost");
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(final int port, final int adminPort) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(ClusterMetaDataAfterNodeLeftTestApp.class.getName());

    String testEventsL1Client = ClusterEventsL1Client.class.getName();
    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testEventsL1Client);

    String testEventsListenerClass = ClusterEventsTestListener.class.getName();
    InstrumentedClassConfigBuilder instrumented3 = new InstrumentedClassConfigBuilderImpl();
    instrumented3.setClassExpression(testEventsListenerClass);

    String testStateClass = ClusterEventsTestState.class.getName();
    InstrumentedClassConfigBuilder instrumented4 = new InstrumentedClassConfigBuilderImpl();
    instrumented4.setClassExpression(testStateClass);

    out.getApplication().getDSO().setInstrumentedClasses(
                                                         new InstrumentedClassConfigBuilder[] { instrumented1,
                                                             instrumented2, instrumented3, instrumented4 });

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(testStateClass + ".listeners");
    root.setRootName("listeners");

    out.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root });

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    lock1.setMethodExpression("* " + ClusterMetaDataAfterNodeLeftTestApp.class.getName() + "*.*(..)");

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    lock2.setMethodExpression("* " + testEventsListenerClass + "*.*(..)");

    LockConfigBuilder lock3 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock3.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    lock3.setMethodExpression("* " + testStateClass + "*.*(..)");

    out.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2, lock3 });

    return out;
  }
}

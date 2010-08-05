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
import com.tctest.ClusterMetaDataPrefetchTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.ClusterMetaDataTestApp.AbstractMojo;
import com.tctest.ClusterMetaDataTestApp.MyMojo;
import com.tctest.ClusterMetaDataTestApp.SomePojo;
import com.tctest.ClusterMetaDataTestApp.YourMojo;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public class ClusterMetaDataPrefetchTest extends TransparentTestBase {

  private int  port;
  private File configFile;
  private int  adminPort;

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(ClusterMetaDataPrefetchTestApp.NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(ClusterMetaDataPrefetchTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(ClusterMetaDataPrefetchTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(ClusterMetaDataPrefetchTestApp.HOST_NAME, "localhost");
  }

  @Override
  protected Class getApplicationClass() {
    return ClusterMetaDataPrefetchTestApp.class;
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

    out.getClient().setFaultCount(2);

    String testClassName = ClusterMetaDataPrefetchTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    InstrumentedClassConfigBuilder instrumented3 = new InstrumentedClassConfigBuilderImpl();
    instrumented3.setClassExpression(SomePojo.class.getName() + "*");

    InstrumentedClassConfigBuilder instrumented4 = new InstrumentedClassConfigBuilderImpl();
    instrumented4.setClassExpression(AbstractMojo.class.getName() + "*");

    InstrumentedClassConfigBuilder instrumented5 = new InstrumentedClassConfigBuilderImpl();
    instrumented5.setClassExpression(YourMojo.class.getName() + "*");

    InstrumentedClassConfigBuilder instrumented6 = new InstrumentedClassConfigBuilderImpl();
    instrumented6.setClassExpression(MyMojo.class.getName() + "*");

    out.getApplication().getDSO().setInstrumentedClasses(
                                                         new InstrumentedClassConfigBuilder[] { instrumented1,
                                                             instrumented2, instrumented3, instrumented4,
                                                             instrumented5, instrumented6 });

    RootConfigBuilder map = new RootConfigBuilderImpl(ClusterMetaDataPrefetchTestApp.L1Client.class, "map", "map");
    out.getApplication().getDSO().setRoots(new RootConfigBuilder[] { map });

    LockConfigBuilder l1ClientAutoLocks = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    l1ClientAutoLocks.setMethodExpression("* " + ClusterMetaDataPrefetchTestApp.L1Client.class.getName() + "*.*(..)");
    l1ClientAutoLocks.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    out.getApplication().getDSO().setLocks(new LockConfigBuilder[] { l1ClientAutoLocks });

    return out;
  }
}

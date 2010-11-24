/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class ConfigDefaultPortTest extends TCTestCase {
  private File tcConfig = null;

  public void testConfigDefaultDSOJmxGroupports() {
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                      + "\n      <server name=\"server1\">" + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-logs</logs>"
                      + "\n       <statistics>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-stats</statistics>"
                      + "\n       <dso-port>9510</dso-port>"
                      + "\n       <jmx-port>9520</jmx-port>"
                      + "\n       <l2-group-port>9530</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\">"
                      + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <statistics>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>8510</dso-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n</server>"
                      + "\n      <server name=\"server3\">"
                      + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <statistics>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>7510</dso-port>"
                      + "\n       <l2-group-port>7555</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n</server>"
                      + "\n      <server name=\"server5\">"
                      + "\n       <dso-port>65534</dso-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n</server>"

                      + "\n      <server name=\"server4\">"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n</server>" + "\n</servers>" + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                                    new FatalIllegalConfigurationChangeHandler());

      L2ConfigurationSetupManager configSetupMgr = null;

      // case 1: all ports specified in the config
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().dsoPort().getIntValue());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().l2GroupPort().getIntValue());

      // case 2: just dso-port specified in the config; other port numbers are calculated
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(8510, configSetupMgr.dsoL2Config().dsoPort().getIntValue());
      Assert.assertEquals(8510 + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT, configSetupMgr.commonl2Config()
          .jmxPort().getIntValue());
      Assert.assertEquals(8510 + L2DSOConfig.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT, configSetupMgr.dsoL2Config()
          .l2GroupPort().getIntValue());

      // case 3: dso-port and group-port specified; jmx-port calculated
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server3");
      Assert.assertEquals(7510, configSetupMgr.dsoL2Config().dsoPort().getIntValue());
      Assert.assertEquals(7520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(7555, configSetupMgr.dsoL2Config().l2GroupPort().getIntValue());

      // case 4: all ports are default
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server4");
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().dsoPort().getIntValue());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().l2GroupPort().getIntValue());

      // case 5: ports range overflow
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server5");
      Assert.assertEquals(65534, configSetupMgr.dsoL2Config().dsoPort().getIntValue());
      Assert
          .assertEquals(
                        ((65534 + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT) % CommonL2Config.MAX_PORTNUMBER)
                            + CommonL2Config.MIN_PORTNUMBER, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert
          .assertEquals(
                        ((65534 + L2DSOConfig.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT) % CommonL2Config.MAX_PORTNUMBER)
                            + CommonL2Config.MIN_PORTNUMBER, configSetupMgr.dsoL2Config().l2GroupPort().getIntValue());

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  @Override
  protected boolean cleanTempDir() {
    return false;
  }

}

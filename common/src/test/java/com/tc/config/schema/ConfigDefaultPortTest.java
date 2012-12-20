/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.object.config.schema.L2DSOConfigObject;
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
                      + "\n       <tsa-port>9510</tsa-port>"
                      + "\n       <jmx-port>9520</jmx-port>"
                      + "\n       <tsa-group-port>9530</tsa-group-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\">"
                      + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <tsa-port>8510</tsa-port>"
                      + "\n</server>"
                      + "\n      <server name=\"server3\">"
                      + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <tsa-port>7510</tsa-port>"
                      + "\n       <tsa-group-port>7555</tsa-group-port>"
                      + "\n</server>"
                      + "\n      <server name=\"server5\">"
                      + "\n       <tsa-port>65534</tsa-port>"
                      + "\n</server>"

                      + "\n      <server name=\"server4\">"
                      + "\n</server>" + "\n</servers>" + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                              new FatalIllegalConfigurationChangeHandler());

      L2ConfigurationSetupManager configSetupMgr = null;

      // case 1: all ports specified in the config
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());

      // case 2: just tsa-port specified in the config; other port numbers are calculated
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(8510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(8510 + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT, configSetupMgr.commonl2Config()
          .jmxPort().getIntValue());
      Assert.assertEquals(8510 + L2DSOConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT, configSetupMgr.dsoL2Config()
          .tsaGroupPort().getIntValue());

      // case 3: tsa-port and group-port specified; jmx-port calculated
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server3");
      Assert.assertEquals(7510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(7520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(7555, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());

      // case 4: all ports are default
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server4");
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());

      // case 5: ports range overflow
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server5");
      Assert.assertEquals(65534, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert
          .assertEquals(((65534 + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT) % CommonL2Config.MAX_PORTNUMBER)
                        + CommonL2Config.MIN_PORTNUMBER, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert
          .assertEquals(((65534 + L2DSOConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT) % CommonL2Config.MAX_PORTNUMBER)
                          + CommonL2Config.MIN_PORTNUMBER, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());

    } catch (Throwable e) {
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

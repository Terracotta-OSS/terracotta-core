/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.terracottatech.config.HaMode;

import java.io.File;
import java.io.FileOutputStream;

public class HaConfigTest extends TCTestCase {
  private File tcConfig = null;

  public void testBasicMakeAllNodes() {
    try {
      tcConfig = getTempFile("tc-config-testFakeL2sName.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                      + "\n      <server name=\"server1\">" + "\n      <dso>" + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" + "\n        </persistence>" + "\n      </dso>"
                      + "\n      </server>" + "\n</servers>" + "\n</tc:tc-config>";
      writeConfigFile(config);

      // test for picking up default active server group
      TVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(
                                                                                                    new String[] {
                                                                                                        "-f",
                                                                                                        tcConfig
                                                                                                            .getAbsolutePath() },
                                                                                                    StandardTVSConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      HaConfig haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
      Assert.assertTrue(haConfig.getNodesStore().getAllNodes().length == 1);

      // test for picking up right active server group for a give server
      factory = new StandardTVSConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server1" }, StandardTVSConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new FatalIllegalConfigurationChangeHandler());
      haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
      Assert.assertTrue(haConfig.getNodesStore().getAllNodes().length == 1);

      // expecting an error when given non existing server for haConfig
      factory = new StandardTVSConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server2" }, StandardTVSConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new FatalIllegalConfigurationChangeHandler());
      try {
        new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
        throw new AssertionError("Config setup manager is suppose to blast for non-existing server name");
      } catch (ConfigurationSetupException cse) {
        // expected exception
      }

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testHaMode() {
    try {
      tcConfig = getTempFile("tc-config-testHaMode1.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"server2\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, configSetupMgr.haConfig().getHa().getMode());
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, configSetupMgr.haConfig().getHa().getMode());
      
      
      tcConfig = getTempFile("tc-config-testHaMode2.xml");
      config =        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"server2\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <ha>" 
                      + "\n          <mode>networked-active-passive</mode>"
                      + "\n             <networked-active-passive>"
                      + "\n                 <election-time>5</election-time>"
                      + "\n             </networked-active-passive> "
                      + "\n      </ha>"
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, configSetupMgr.haConfig().getHa().getMode());
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, configSetupMgr.haConfig().getHa().getMode());
      
      tcConfig = getTempFile("tc-config-testHaMode3.xml");
      config =        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"server2\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <ha>" 
                      + "\n          <mode>disk-based-active-passive</mode>"
                      + "\n      </ha>"
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(HaMode.DISK_BASED_ACTIVE_PASSIVE, configSetupMgr.haConfig().getHa().getMode());
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(HaMode.DISK_BASED_ACTIVE_PASSIVE, configSetupMgr.haConfig().getHa().getMode());
      
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

}

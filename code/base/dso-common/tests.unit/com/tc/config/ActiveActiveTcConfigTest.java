/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.MembersConfig;
import com.tc.config.schema.NewHaConfigObject;
import com.tc.config.schema.defaults.FromSchemaDefaultValueProvider;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.terracottatech.config.Ha;
import com.terracottatech.config.Servers;

import java.io.File;
import java.io.FileOutputStream;

public class ActiveActiveTcConfigTest extends TCTestCase {
  private File tcConfig = null;
  
  public void testHA1(){
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">"
                      + "\n       <data>/Users/rsingh/terracotta/server1-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server1-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server1-stats</statistics>"
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
                      + "\n       <data>/Users/rsingh/terracotta/server2-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server2-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>9511</dso-port>"
                      + "\n       <jmx-port>9521</jmx-port>"
                      + "\n       <l2-group-port>9531</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>" 
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>" 
                      + "\n      </dso>" 
                      + "\n</server>" 
                      + "\n<active-server-groups>" 
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server1</member>"
                      + "\n     </members>"
                      + "\n   <ha>" 
                      + "\n       <mode>networked-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>1234</election-time>"
                      + "\n       </networked-active-passive>" 
                      + "\n   </ha>"
                      + "\n   </active-server-group>"
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server2</member>"
                      + "\n     </members>"
                      + "\n   <ha>" 
                      + "\n       <mode>disk-based-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>3451</election-time>"
                      + "\n       </networked-active-passive>" 
                      + "\n   </ha>"
                      + "\n   </active-server-group>"
                      + "\n</active-server-groups>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(1234, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("networked-active-passive"));
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(3451, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("disk-based-active-passive"));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testHA2(){
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">"
                      + "\n       <data>/Users/rsingh/terracotta/server1-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server1-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server1-stats</statistics>"
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
                      + "\n       <data>/Users/rsingh/terracotta/server2-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server2-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>9511</dso-port>"
                      + "\n       <jmx-port>9521</jmx-port>"
                      + "\n       <l2-group-port>9531</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>" 
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>" 
                      + "\n      </dso>" 
                      + "\n</server>" 
                      + "\n<active-server-groups>" 
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server1</member>"
                      + "\n     </members>"
                      + "\n   <ha>" 
                      + "\n       <mode>networked-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>1234</election-time>"
                      + "\n       </networked-active-passive>" 
                      + "\n   </ha>"
                      + "\n   </active-server-group>"
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server2</member>"
                      + "\n     </members>"
                      + "\n   <ha>" 
                      + "\n       <mode>disk-based-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>3451</election-time>"
                      + "\n       </networked-active-passive>" 
                      + "\n   </ha>"
                      + "\n   </active-server-group>"
                      + "\n</active-server-groups>"
                      + "\n<ha>"
                      + "\n       <mode>networked-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>7891</election-time>"
                      + "\n       </networked-active-passive>"
                      + "\n</ha>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(1234, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("networked-active-passive"));
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(3451, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("disk-based-active-passive"));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testHA3(){
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">"
                      + "\n       <data>/Users/rsingh/terracotta/server1-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server1-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server1-stats</statistics>"
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
                      + "\n       <data>/Users/rsingh/terracotta/server2-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server2-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>9511</dso-port>"
                      + "\n       <jmx-port>9521</jmx-port>"
                      + "\n       <l2-group-port>9531</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>" 
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>" 
                      + "\n      </dso>" 
                      + "\n</server>" 
                      + "\n<active-server-groups>" 
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server1</member>"
                      + "\n     </members>"
                      + "\n   <ha>" 
                      + "\n       <mode>networked-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>1234</election-time>"
                      + "\n       </networked-active-passive>" 
                      + "\n   </ha>"
                      + "\n   </active-server-group>"
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server2</member>"
                      + "\n     </members>"
                      + "\n   </active-server-group>"
                      + "\n</active-server-groups>"
                      + "\n<ha>"
                      + "\n       <mode>networked-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>7891</election-time>"
                      + "\n       </networked-active-passive>"
                      + "\n</ha>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals(1234, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("networked-active-passive"));
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(7891, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("networked-active-passive"));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testHA4(){
    
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">"
                      + "\n       <data>/Users/rsingh/terracotta/server1-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server1-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server1-stats</statistics>"
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
                      + "\n       <data>/Users/rsingh/terracotta/server2-data</data>"
                      + "\n       <logs>/Users/rsingh/terracotta/server2-logs</logs>"
                      + "\n       <statistics>/Users/rsingh/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>9511</dso-port>"
                      + "\n       <jmx-port>9521</jmx-port>"
                      + "\n       <l2-group-port>9531</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>" 
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>" 
                      + "\n      </dso>" 
                      + "\n</server>" 
                      + "\n<active-server-groups>" 
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server1</member>"
                      + "\n     </members>"
                      + "\n   <ha>" 
                      + "\n       <mode>networked-active-passive</mode>"
                      + "\n       <networked-active-passive>"
                      + "\n         <election-time>1234</election-time>"
                      + "\n       </networked-active-passive>" 
                      + "\n   </ha>"
                      + "\n   </active-server-group>"
                      + "\n   <active-server-group>"
                      + "\n     <members>" 
                      + "\n       <member>server2</member>"
                      + "\n     </members>"
                      + "\n   </active-server-group>"
                      + "\n</active-server-groups>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Ha ha = NewHaConfigObject.getDefaultCommonHa(new FromSchemaDefaultValueProvider(), new StandardBeanRepository(Servers.class));

      Assert.assertEquals(1234, configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals("networked-active-passive"));
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals(ha.getNetworkedActivePassive().getElectionTime(), configSetupMgr.haConfig().electionTime());
      Assert.eval(configSetupMgr.haConfig().haMode().equals(ha.getMode().toString()));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testFakeL2sName() {
    try {
      tcConfig = getTempFile("tc-config-testFakeL2sName.xml");
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
                      + "\n      </dso>" + "\n      </server>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError("Parsing of tc-config succeeded with fake sever names");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testL2sWithDifferentPersistenceMode() {
    try {
      tcConfig = getTempFile("tc-config-testL2sWithDifferentPersistenceMode.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"localhost\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso>"
                      + "\n        <persistence>" 
                      + "\n          <mode>temporary-swap-only</mode>"
                      + "\n        </persistence>" 
                      + "\n      </dso>" 
                      + "\n      </server>" + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError("Parsing of tc-config succeeded with servers having different persistence mode");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testServerInTwoGroups() {
    try {
      tcConfig = getTempFile("tc-config-testServerInTwoGroups.xml");
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
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server3</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError(
                               "Parsing of tc-config succeeded with the same server in muliple active-server-group element");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testServerNotPresentInAnyGroup() {
    try {
      tcConfig = getTempFile("tc-config-testServerNotPresentInAnyGroup.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server5\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server3</member>"
                      + "\n                <member>server4</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError(
                               "Parsing of tc-config succeeded with the server element not present in any active-server-group element");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testMultipleGroups() {
    try {
      tcConfig = getTempFile("tc-config-testMultipleGroups.xml");
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
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>"
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError("Parsing of tc-config succeeded with two active-server-gropus elements.");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testMultipleHa() {
    try {
      tcConfig = getTempFile("tc-config-testMultipleHa.xml");
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
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n      <ha>"
                      + "\n        <mode>networked-active-passive</mode>" 
                      + "\n        <networked-active-passive>"
                      + "\n          <election-time>1000</election-time>" 
                      + "\n        </networked-active-passive>"
                      + "\n      </ha>" 
                      + "\n      <ha>" 
                      + "\n        <mode>networked-active-passive</mode>"
                      + "\n        <networked-active-passive>" 
                      + "\n          <election-time>1000</election-time>"
                      + "\n        </networked-active-passive>" 
                      + "\n      </ha>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError("Parsing of tc-config succeeded with two ha elements.");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testMultipleUpdateCheck() {
    try {
      tcConfig = getTempFile("tc-config-testMultipleUpdateCheck.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n    <server name=\"server1\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n    </server>" 
                      + "\n    <active-server-groups>" 
                      + "\n        <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n            <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n            </ha>"
                      + "\n        </active-server-group>" 
                      + "\n    </active-server-groups>" 
                      + "\n    <ha>"
                      + "\n      <mode>networked-active-passive</mode>" 
                      + "\n      <networked-active-passive>"
                      + "\n        <election-time>1000</election-time>" 
                      + "\n      </networked-active-passive>"
                      + "\n    </ha>" 
                      + "\n    <update-check>" 
                      + "\n      <enabled>true</enabled>"
                      + "\n      <period-days>100</period-days>" 
                      + "\n    </update-check>" 
                      + "\n    <update-check>"
                      + "\n      <enabled>true</enabled>" 
                      + "\n      <period-days>100</period-days>"
                      + "\n    </update-check>" 
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      throw new AssertionError("Parsing of tc-config succeeded with two update-check elements.");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testParseGroupInOrder() {
    try {
      tcConfig = getTempFile("tc-config-testParseGroupInOrder.xml");
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
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n        </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server3</member>"
                      + "\n                <member>server4</member>" 
                      + "\n                <member>server5</member>"
                      + "\n              </members>" 
                      + "\n              <ha>"
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n        </active-server-group>" 
                      + "\n        <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server6</member>"
                      + "\n                <member>server7</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n        </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager l2TVSConfigurationSetupManager = factory
          .createL2TVSConfigurationSetupManager(tcConfig, null);
      ActiveServerGroupConfig[] activeServerGroup = l2TVSConfigurationSetupManager.activeServerGroupsConfig()
          .getActiveServerGroupArray();

      int numberOfGroups = activeServerGroup.length;
      int serverNumber = 1;
      for (int i = 0; i < numberOfGroups; i++) {
        String[] groupMembers = activeServerGroup[i].getMembers().getMemberArray();
        for (int j = 0; j < groupMembers.length; j++) {
          Assert.eval(groupMembers[j].equals("server" + serverNumber++));
        }
      }
    } catch (ConfigurationSetupException e) {
      throw new AssertionError(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testDefaultValues() {
    try {
      tcConfig = getTempFile("tc-config-testDefaultValues.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager setUpManager = factory.createL2TVSConfigurationSetupManager(tcConfig, null);

      // Check for default values
      ActiveServerGroupsConfig asgs = setUpManager.activeServerGroupsConfig();
      ActiveServerGroupConfig[] grps = asgs.getActiveServerGroupArray();

      Assert.assertEquals(1, grps.length);
      Assert.assertEquals(0, grps[0].getId());
      Assert.assertNotNull(grps[0].getHa());
      Assert.assertEquals(5, grps[0].getHa().electionTime());

      MembersConfig members = grps[0].getMembers();
      Assert.assertNotNull(members);
      String[] memberNames = members.getMemberArray();
      Assert.assertNotNull(memberNames);
      Assert.assertEquals(1, memberNames.length);
      Assert.assertNotNull(memberNames[0]);

      Assert.assertNotNull(setUpManager.updateCheckConfig());
      Assert.assertNotNull(setUpManager.haConfig());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testGroupNames() {
    try {
      tcConfig = getTempFile("tc-config-testGroupNames.xml");
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
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group group-name=\"coordinator-grp\">"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server2</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      
      L2TVSConfigurationSetupManager setupManager = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      ActiveServerGroupConfig asgf = setupManager.getActiveServerGroupForThisL2();
      Assert.assertEquals("coordinator-grp", asgf.getGroupName());
      setupManager = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      asgf = setupManager.getActiveServerGroupForThisL2();
      Assert.assertEquals("server2", asgf.getGroupName());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void testSameGroupNames() {
    try {
      tcConfig = getTempFile("tc-config-testSameGroupNames.xml");
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
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group group-name=\"coordinator-grp\">"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group group-name=\"coordinator-grp\">"
                      + "\n              <members>" 
                      + "\n                <member>server2</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      
      factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      fail("Should throw exception when the group names are same");
    } catch (ConfigurationSetupException e) { 
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testSameGroupNames2() {
    try {
      tcConfig = getTempFile("tc-config-testSameGroupNames2.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"a\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"b\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group group-name=\"b\">"
                      + "\n              <members>" 
                      + "\n                <member>a</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>b</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1000</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      
      factory.createL2TVSConfigurationSetupManager(tcConfig, "a");
      fail("Should throw exception when the group names are same");
    } catch (ConfigurationSetupException e) { 
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
  
  public void testModesOfServer() {
    try {
      tcConfig = getTempFile("tc-config-testModesOfServer.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server host=\"%i\" name=\"server1\">"
                      + "\n      <jmx-port>10000</jmx-port>"
                      + "\n      <dso-port>11000</dso-port>"
                      + "\n      <l2-group-port>12000</l2-group-port>"
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server host=\"%i\" name=\"server2\">" 
                      + "\n      <jmx-port>10001</jmx-port>"
                      + "\n      <dso-port>11001</dso-port>"
                      + "\n      <l2-group-port>12001</l2-group-port>"
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>temporary-swap-only</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>5</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server2</member>"
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>5</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
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

  protected boolean cleanTempDir() {
    return false;
  }
}

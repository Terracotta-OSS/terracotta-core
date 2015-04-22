/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.config;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.SecurityConfig;
import com.tc.config.schema.UpdateCheckConfig;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.config.test.schema.GroupConfigBuilder;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.L2SConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ConfigInfoFromL2Test extends BaseDSOTestCase {
  private static int                    STRIPE_COUNT    = 16;
  protected final TCThreadGroup         group           = new TCThreadGroup(
                                                                            new ThrowableHandlerImpl(
                                                                                                 TCLogging
                                                                                                     .getLogger(DistributedObjectServer.class)));

  private TCServer                      tcserver;
  private final TerracottaConfigBuilder tcConfigBuilder = createConfig();
  private File                          tcConfig        = null;

  @Override
  public void setUp() throws Exception {
    final PortChooser pc = new PortChooser();
    final int tsaPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    L2ConfigBuilder l2Config = tcConfigBuilder.getServers().getL2s()[0];
    l2Config.setTSAPort(tsaPort);
    l2Config.setJMXPort(jmxPort);
    tcConfig = getTempFile("tc-config-testFakeL2sName.xml");
    writeConfigFile(tcConfigBuilder.toString());

    this.tcserver = startupServer();
    configFactory().addServerToL1Config("127.0.0.1", tsaPort, jmxPort);
  }

  @Override
  public void tearDown() {
    this.tcserver.stop();
  }

  public void testGetGroupNameIDMapFromL2() throws Exception {

    L1ConfigurationSetupManager manager = super.createL1ConfigManager();
    ConfigInfoFromL2 configInfoFromL2 = new ConfigInfoFromL2Impl(manager);
    Map<String, GroupID> map = configInfoFromL2.getGroupNameIDMapFromL2();
    Assert.assertEquals(STRIPE_COUNT, map.size());
    for (Map.Entry<String, GroupID> entry : map.entrySet()) {
      System.out.println("Groupname to ID map " + entry.getKey() + " --> " + entry.getValue());
    }
    for (Map.Entry<String, GroupID> entry : map.entrySet()) {
      Assert.assertEquals(entry.getKey(), "Tc-Group-" + entry.getValue().toInt());
    }
  }

  protected TCServer startupServer() {
    StartAction start_action = new StartAction();
    new StartupHelper(this.group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  private class StartAction implements StartupHelper.StartupAction {
    private TCServer server = null;

    private StartAction() {
      //
    }

    public TCServer getServer() {
      return server;
    }

    @Override
    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      TestConfigurationSetupManagerFactory factory = configFactory();
      L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(tcConfig, "server0");

      server = new TCServerImpl(new WrapL2ConfigurationSetupManager(manager));
      server.start();
    }
  }

  private TerracottaConfigBuilder createConfig() {
    final PortChooser pc = new PortChooser();
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();
    L2SConfigBuilder l2sBuilder = new L2SConfigBuilder();

    GroupConfigBuilder[] groupBuilders = new GroupConfigBuilder[STRIPE_COUNT];
    for (int i = 0; i < STRIPE_COUNT; ++i) {
      groupBuilders[i] = new GroupConfigBuilder(null);

      L2ConfigBuilder l2Builder = new L2ConfigBuilder();
      l2Builder.setName("server" + i);
      l2Builder.setJMXPort(pc.chooseRandomPort());
      l2Builder.setJMXBindAddress("127.0.0.1");
      l2Builder.setTSAPort(pc.chooseRandomPort());
      l2Builder.setTSABindAddress("127.0.0.1");
      l2Builder.setTSAGroupPort(pc.chooseRandomPort());
      l2Builder.setTSAGroupPortBindAddress("127.0.0.1");
      l2Builder.setOffHeapEnabled(true);
      l2Builder.setOffHeapMaxDataSize("512m");

      groupBuilders[i].setL2s(new L2ConfigBuilder[] { l2Builder });
    }

    l2sBuilder.setGroups(groupBuilders);
    out.setServers(l2sBuilder);

    return out;
  }

  private void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private static class WrapActiveServerGroupsConfig implements ActiveServerGroupsConfig {

    private final ActiveServerGroupsConfig config;

    private WrapActiveServerGroupsConfig(ActiveServerGroupsConfig config) {
      this.config = config;
    }

    @Override
    public List<ActiveServerGroupConfig> getActiveServerGroups() {
      return this.config.getActiveServerGroups();
    }

    /*
     * Run non-active-active mode but with 16 stripes. Hack this method to return 1 only if called by isActiveActive. So
     * that to avoid active-active stuffs. Such as license check, starting all stripes and etc.
     */
    @Override
    public int getActiveServerGroupCount() {
      Throwable t = new Throwable();
      StackTraceElement[] js = t.getStackTrace();
      if (js[1].getMethodName().equals("isActiveActive")) {
        // on purpose to return 1, so that not in active-active
        return 1;
      } else {
        return this.config.getActiveServerGroupCount();
      }
    }

    @Override
    public ActiveServerGroupConfig getActiveServerGroupForL2(String l2Name) {
      return this.config.getActiveServerGroupForL2(l2Name);
    }

    @Override
    public void changesInItemForbidden(ConfigItem item) {
      this.config.changesInItemForbidden(item);
    }

    @Override
    public void changesInItemIgnored(ConfigItem item) {
      this.config.changesInItemIgnored(item);
    }

    @Override
    public XmlObject getBean() {
      return this.config.getBean();
    }

  }

  private static class WrapL2ConfigurationSetupManager implements L2ConfigurationSetupManager {
    private final L2ConfigurationSetupManager l2Config;

    private WrapL2ConfigurationSetupManager(L2ConfigurationSetupManager l2Config) {
      this.l2Config = l2Config;
    }

    // wrap here to provide hacked ActiveServerGroupsConfig for testing purpose
    @Override
    public ActiveServerGroupsConfig activeServerGroupsConfig() {
      return new WrapActiveServerGroupsConfig(this.l2Config.activeServerGroupsConfig());
    }

    @Override
    public String[] allCurrentlyKnownServers() {
      return this.l2Config.allCurrentlyKnownServers();
    }

    @Override
    public CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException {
      return this.l2Config.commonL2ConfigFor(name);
    }

    @Override
    public CommonL2Config commonl2Config() {
      return this.l2Config.commonl2Config();
    }

    @Override
    public String describeSources() {
      return this.l2Config.describeSources();
    }

    @Override
    public L2DSOConfig dsoL2Config() {
      return this.l2Config.dsoL2Config();
    }

    @Override
    public L2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException {
      return this.l2Config.dsoL2ConfigFor(name);
    }

    @Override
    public InputStream effectiveConfigFile() {
      return this.l2Config.effectiveConfigFile();
    }

    @Override
    public ActiveServerGroupConfig getActiveServerGroupForThisL2() {
      return this.l2Config.getActiveServerGroupForThisL2();
    }

    @Override
    public String getL2Identifier() {
      return this.l2Config.getL2Identifier();
    }

    @Override
    public SecurityConfig getSecurity() {
      return this.l2Config.getSecurity();
    }

    @Override
    public String[] processArguments() {
      return this.l2Config.processArguments();
    }

    @Override
    public InputStream rawConfigFile() {
      return this.l2Config.rawConfigFile();
    }

    @Override
    public TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator,
                                                    TerracottaOperatorEventLogger opEventLogger)
        throws ConfigurationSetupException {
      return this.l2Config.reloadConfiguration(serverConnectionValidator, opEventLogger);
    }

    @Override
    public boolean isSecure() {
      return this.l2Config.isSecure();
    }

    @Override
    public UpdateCheckConfig updateCheckConfig() {
      return this.l2Config.updateCheckConfig();
    }

  }

}

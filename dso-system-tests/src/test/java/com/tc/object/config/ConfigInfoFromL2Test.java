/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.HaConfigSchema;
import com.tc.config.schema.SystemConfig;
import com.tc.config.schema.UpdateCheckConfig;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.config.test.schema.GroupConfigBuilder;
import com.tc.config.test.schema.GroupsConfigBuilder;
import com.tc.config.test.schema.HaConfigBuilder;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.L2SConfigBuilder;
import com.tc.config.test.schema.MembersConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.schema.DSOApplicationConfig;
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
import java.util.Map;

public class ConfigInfoFromL2Test extends BaseDSOTestCase {
  private static int                    STRIPE_COUNT    = 16;
  protected final TCThreadGroup         group           = new TCThreadGroup(
                                                                            new ThrowableHandler(
                                                                                                 TCLogging
                                                                                                     .getLogger(DistributedObjectServer.class)));

  private TCServer                      tcserver;
  private final TerracottaConfigBuilder tcConfigBuilder = createConfig();
  private File                          tcConfig        = null;

  @Override
  public void setUp() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    L2ConfigBuilder l2Config = tcConfigBuilder.getServers().getL2s()[0];
    l2Config.setDSOPort(dsoPort);
    l2Config.setJMXPort(jmxPort);
    tcConfig = getTempFile("tc-config-testFakeL2sName.xml");
    writeConfigFile(tcConfigBuilder.toString());

    this.tcserver = startupServer();
    configFactory().addServerToL1Config("127.0.0.1", dsoPort, jmxPort);
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

    L2ConfigBuilder[] l2Builders = new L2ConfigBuilder[STRIPE_COUNT];
    GroupsConfigBuilder groupsBuilder = new GroupsConfigBuilder();
    for (int i = 0; i < STRIPE_COUNT; ++i) {
      L2ConfigBuilder l2Builder = new L2ConfigBuilder();
      l2Builder.setName("server" + i);
      l2Builder.setJMXPort(pc.chooseRandomPort());
      l2Builder.setJMXBindAddress("127.0.0.1");
      l2Builder.setDSOPort(pc.chooseRandomPort());
      l2Builder.setDSOBindAddress("127.0.0.1");
      l2Builder.setL2GroupPort(pc.chooseRandomPort());
      l2Builder.setL2GroupPortBindAddress("127.0.0.1");
      l2Builders[i] = l2Builder;

      MembersConfigBuilder memberBuilder = new MembersConfigBuilder();
      memberBuilder.addMember("server" + i);
      GroupConfigBuilder groupBuilder = new GroupConfigBuilder(null);
      groupBuilder.setMembers(memberBuilder);
      groupsBuilder.addGroupConfigBuilder(groupBuilder);
    }

    l2sBuilder.setL2s(l2Builders);
    HaConfigBuilder haConfigBuilder = new HaConfigBuilder();
    haConfigBuilder.setMode(HaConfigBuilder.HA_MODE_NETWORKED_ACTIVE_PASSIVE);
    l2sBuilder.setHa(haConfigBuilder);
    l2sBuilder.setGroups(groupsBuilder);
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

    public ActiveServerGroupConfig[] getActiveServerGroupArray() {
      return this.config.getActiveServerGroupArray();
    }

    /*
     * Run non-active-active mode but with 16 stripes. Hack this method to return 1 only if called by isActiveActive. So
     * that to avoid active-active stuffs. Such as license check, starting all stripes and etc.
     */
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

    public ActiveServerGroupConfig getActiveServerGroupForL2(String l2Name) {
      return this.config.getActiveServerGroupForL2(l2Name);
    }

    public void changesInItemForbidden(ConfigItem item) {
      this.config.changesInItemForbidden(item);
    }

    public void changesInItemIgnored(ConfigItem item) {
      this.config.changesInItemIgnored(item);
    }

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
    public ActiveServerGroupsConfig activeServerGroupsConfig() {
      return new WrapActiveServerGroupsConfig(this.l2Config.activeServerGroupsConfig());
    }

    public String[] allCurrentlyKnownServers() {
      return this.l2Config.allCurrentlyKnownServers();
    }

    public String[] applicationNames() {
      return this.l2Config.applicationNames();
    }

    public CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException {
      return this.l2Config.commonL2ConfigFor(name);
    }

    public CommonL2Config commonl2Config() {
      return this.l2Config.commonl2Config();
    }

    public String describeSources() {
      return this.l2Config.describeSources();
    }

    public DSOApplicationConfig dsoApplicationConfigFor(String applicationName) {
      return this.l2Config.dsoApplicationConfigFor(applicationName);
    }

    public L2DSOConfig dsoL2Config() {
      return this.l2Config.dsoL2Config();
    }

    public L2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException {
      return this.l2Config.dsoL2ConfigFor(name);
    }

    public InputStream effectiveConfigFile() {
      return this.l2Config.effectiveConfigFile();
    }

    public ActiveServerGroupConfig getActiveServerGroupForThisL2() {
      return this.l2Config.getActiveServerGroupForThisL2();
    }

    public String getL2Identifier() {
      return this.l2Config.getL2Identifier();
    }

    public HaConfigSchema haConfig() {
      return this.l2Config.haConfig();
    }

    public String[] processArguments() {
      return this.l2Config.processArguments();
    }

    public InputStream rawConfigFile() {
      return this.l2Config.rawConfigFile();
    }

    public TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator,
                                                    TerracottaOperatorEventLogger opEventLogger)
        throws ConfigurationSetupException {
      return this.l2Config.reloadConfiguration(serverConnectionValidator, opEventLogger);
    }

    public SystemConfig systemConfig() {
      return this.l2Config.systemConfig();
    }

    public UpdateCheckConfig updateCheckConfig() {
      return this.l2Config.updateCheckConfig();
    }

  }

}

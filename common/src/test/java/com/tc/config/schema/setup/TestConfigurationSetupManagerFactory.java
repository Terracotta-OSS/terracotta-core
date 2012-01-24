/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.common.XPath;

import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.HaConfigSchema;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.SystemConfig;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory.ConfigMode;
import com.tc.object.config.schema.L1DSOConfig;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.test.GroupData;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.Members;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Offheap;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Property;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument.TcConfig;
import com.terracottatech.config.TcProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A {@link com.tc.config.schema.setup.ConfigurationSetupManagerFactory} that creates config appropriate for usage in
 * tests. This config behaves just like normal config, except that it reads no files; everything is in-memory instead.
 * You can specify whether you want this config to act like centralized config (all at L2), or distributed config (every
 * L1 has its own copy of the config, too). </p>
 * <p>
 * To use this class, simply get the appropriate config object that you need by calling a method (<em>e.g.</em>,
 * {@link #systemConfig()}). Then, call a method on it (like {@link com.tc.config.schema.SystemConfig#dsoEnabled()}, for
 * example); this will give you back a {@link ConfigItem}, or a subinterface thereof. Cast this item to a
 * {@link com.tc.config.schema.SettableConfigItem}, and then call {@link SettableConfigItem#setValue(Object)} on it (or
 * one of the similar methods that takes a primitive type), passing it the value you want this class to return for that
 * item. Make sure you get the class right &mdash; if you don't, you'll get a nasty {@link ClassCastException} when
 * production code tries to access the value in that {@link ConfigItem}.
 * </p>
 * <p>
 * Only one little trick: if you're setting something that's a complex object &mdash; <em>i.e.</em>, not just a
 * primitive type, {@link String}, array of {@link String}s or something like that (specifically, the object types
 * returned by the top-level subclasses of {@link com.tc.config.schema.dynamic.XPathBasedConfigItem}) &mdash; then you
 * need to set an implementation of {@link XmlObject}, not the actual Terracotta-defined types that the real
 * {@link ConfigItem}s return. (This is because we're using the real config system &mdash; see below for details &mdash;
 * and it expects {@link XmlObject}s of the appropriate type so it can translate them to the Terracotta-defined types
 * that we really return.) Fortunately, all XML beans have <code>Factory</code> inner classes that will let you create
 * them. If you then wrap these calls in a function and reuse it, you'll be in fine shape if/when the actual XML beans
 * are changed.
 * </p>
 * <p>
 * Note: There is no support yet for different L1s having different config, or config that differs from L2's.
 * </p>
 * <h3>Maintenance:</h3>
 * <p>
 * If you create new typed subinterfaces of {@link ConfigItem}, you do need to make
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler.OurSettableConfigItem} implement them. Don't worry,
 * though; the methods can just throw {@link com.tc.util.TCAssertionError}, and don't need to (nor should they) actually
 * do anything.
 * </p>
 * <p>
 * If you introduce new config objects or new beans to the system, you'll need to do a lot more, but, then, presumably
 * if you're doing that you understand more about the way the config system works than this comment is going to tell
 * you, even if it is long.
 * </p>
 * <p>
 * That's it. In particular, there's no need to actually do anything when you add new items to existing config objects:
 * this is significant.
 * </p>
 * <h3>How it works:</h3>
 * <p>
 * How this all works is a little interesting. It's involved, but it buys us two hugely useful properties: clients use
 * basically the same APIs to change config parameters as they do to get them, and we need to do zero (really!) work
 * when new {@link ConfigItem}s are added to config objects. This means it's impossible for test code to get
 * "out-of-sync" with respect to config, so it always works, and Eclipse's refactoring tools also work.
 * </p>
 * <p>
 * First, a little overview: for our test config, we use the real production config system, all the way down to the
 * level of the actual {@link XmlObject}s that get stuffed in the config system's
 * {@link com.tc.config.schema.repository.BeanRepository} objects. <em>Those</em> are realy {@link XmlObject}s of the
 * appropriate type, created by the {@link TestConfigBeanSet} and modified by calls to the pseudo-'config objects' that
 * this class exposes. However, everything else is real: you're exercising real config code, real XPath-based
 * {@link ConfigItem}s reading from real {@link XmlObject}s, real L1-L2 protocol handling, and so on. This has many
 * benefits, including making your tests behave more closely to the way real production code works (a good thing), and
 * exercising more of the config system in your tests (also a good thing).
 * </p>
 * <p>
 * Details of how it all works:
 * </p>
 * <ul>
 * <li>A {@link TestConfigBeanSet} holds on to the set of all root {@link XmlObject}s that we need to configure the
 * system &mdash; for example, the {@link L1} we use for L1 config, the {@link L2}s representing each L2's config (and
 * the {@link L2S} that wraps them all up together), the {@link com.terracottatech.configV1.System} we use for system
 * config.</li>
 * <li>These {@link XmlObject}s are honest-to-God real instances, as created by their factories (for example,
 * {@link L1.Factory}. At the start, they have just enough configuration populated into them to make sure they validate.
 * </li>
 * <li>This class exposes what look like instances of the normal config objects available to the system. However, these
 * are actually proxies created with {@link java.lang.reflect.Proxy}, using a
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler}.</li>
 * <li>That invocation handler, in response to method calls, parcels out {@link ConfigItem}s that are instances of
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler.OurSettableConfigItem}. When you call
 * <code>setValue</code> on them, they do their magic: using the {@link XPath} they get from the corresponding "sample"
 * {@link ConfigItem} (see below), they descend the tree of {@link XmlObject}s, starting at the root, creating children
 * along the way as necessary, and finally set the correct property on the correct bean. (This is conceptually easy but
 * actually full of all kinds of nasty mess; this is why {@link OurSettableConfigItem} is such a messy class.) .</li>
 * <li>Okay, but how does it know what XPath to use to descend the tree? That's where the "sample" config objects below
 * (fields in this object) come in. They are actual, real config objects that are created around the bean set, before
 * any values are set &mdash; but that doesn't matter, because the only thing we use them for is to get the
 * {@link XPathBasedConfigItem}s out of them and extract the XPath from them. So, when you call the method that gets a
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler.OurSettableConfigItem} from the proxied-up config
 * object, it calls the exact same method on the "sample" config object, grabbing the {@link ConfigItem} returned,
 * casting it to an {@link com.tc.config.schema.dynamic.XPathBasedConfigItem}, and extracting the XPath out of that.</li>
 * </ul>
 * </p>
 * <p>
 * Is this whole thing complicated? Yes, absolutely. Can it probably be simplified? Yes. Is the design bad? I don't
 * think so, and here's why: it gives a very clean, very simple API for setting config values, and it's maintainable.
 * Other potential solutions either tend to hurt in terms of maintenance &mdash; you have to do something whenever you
 * add a config item to the system, and if you mess it up, your new config just gets silently ignored &mdash; or in
 * terms of API &mdash; they create a much more complex (and much harder to maintain) API for setting config values.
 * This way, all the complexity is wrapped in three smallish classes (this one, {@link TestConfigBeanSet}, and
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler}) in the config package in the source tree, and not
 * spread all over the place throughout our code, causing massive pain if we ever have to change anything.
 */
public class TestConfigurationSetupManagerFactory extends BaseConfigurationSetupManagerFactory {

  public static final int                       MODE_CENTRALIZED_CONFIG = 0;
  public static final int                       MODE_DISTRIBUTED_CONFIG = 1;
  public static final String                    DEFAULT_HOST            = "localhost";
  public static final String                    DEFAULT_SERVER_NAME     = "default";

  private final TestConfigurationCreator        configurationCreator;

  private final SystemConfig                    sampleSystem;
  private final CommonL1Config                  sampleL1Common;
  private final L1DSOConfig                     sampleL1DSO;
  private final CommonL2Config                  sampleL2Common;
  private final L2DSOConfig                     sampleL2DSO;
  private final ActiveServerGroupsConfig        sampleActiveServerGroups;
  private final HaConfigSchema                  sampleHa;

  private final String                          defaultL2Identifier;

  private final int                             mode;

  private boolean                               gcEnabled               = true;
  private boolean                               gcVerbose               = false;
  private int                                   gcIntervalInSec         = 3600;

  private boolean                               isConfigDone            = false;
  private boolean                               offHeapEnabled          = false;
  private String                                maxOffHeapDataSize      = "-1m";
  private PersistenceMode.Enum                  persistenceMode         = PersistenceMode.TEMPORARY_SWAP_ONLY;
  private final L1ConfigurationSetupManagerImpl sampleL1Manager;
  private final L2ConfigurationSetupManagerImpl sampleL2Manager;

  public TestConfigurationSetupManagerFactory(int mode, String l2Identifier,
                                              IllegalConfigurationChangeHandler illegalConfigurationChangeHandler)
      throws ConfigurationSetupException {
    super(illegalConfigurationChangeHandler);

    final ConfigBeanFactory configBeanFactory = new TerracottaDomainConfigurationDocumentBeanFactory();
    final ConfigurationSpec configSpec = new ConfigurationSpec("default-tc-config.xml", ConfigMode.L2,
                                                               new File(System.getProperty("user.dir")));
    this.configurationCreator = new TestConfigurationCreator(configSpec, configBeanFactory, true);

    this.mode = mode;
    this.defaultL2Identifier = l2Identifier;

    // FIXME 2005-11-30 andrew -- This stinks like mad...we should be able to do something better than perverting the
    // existing config-setup managers here.

    sampleL2Manager = this.createL2TVSConfigurationSetupManager(null);
    if (this.sampleL2Manager.tcPropertiesRepository().bean() == null) {
      try {
        this.sampleL2Manager.tcPropertiesRepository()
            .setBean(TcProperties.Factory.newInstance(), "from test framework");
      } catch (XmlException e) {
        throw new RuntimeException(e);
      }
    }
    this.sampleSystem = sampleL2Manager.systemConfig();
    this.sampleL2Common = sampleL2Manager.commonl2Config();
    this.sampleL2DSO = sampleL2Manager.dsoL2Config();
    this.sampleActiveServerGroups = sampleL2Manager.activeServerGroupsConfig();
    this.sampleHa = sampleL2Manager.haConfig();

    sampleL1Manager = this.createL1TVSConfigurationSetupManager(this.configurationCreator);// new
    try {
      setServersBeanForL1s((Servers) this.sampleL2Manager.serversBeanRepository().bean(), "from L2");
      if (this.sampleL1Manager.tcPropertiesRepository().bean() == null) {
        this.sampleL1Manager.tcPropertiesRepository()
            .setBean(TcProperties.Factory.newInstance(), "from test framework");
      }
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
    this.sampleL1Common = sampleL1Manager.commonL1Config();
    this.sampleL1DSO = sampleL1Manager.dsoL1Config();

    applyDefaultTestConfig();
  }

  private static final String BOGUS_FILENAME = "nonexistent-directory-SHOULD-NEVER-EXIST/";

  private void applyDefaultTestConfig() {
    // // Use a license that lets us do anything.
    // try {
    // String path = getEverythingLicensePath();
    // ((SettableConfigItem) systemConfig().licenseLocation()).setValue(path);
    // } catch (IOException ioe) {
    // throw Assert.failure("Unable to fetch data directory root to find license for tests.", ioe);
    // }
    //
    // ((SettableConfigItem) systemConfig().licenseType()).setValue(LicenseType.PRODUCTION);
    //
    // // Make servers use dynamic ports, by default.
    // ((SettableConfigItem) l2DSOConfig().listenPort()).setValue(0);
    // ((SettableConfigItem) l2CommonConfig().jmxPort()).setValue(0);

    // We also set the data and log directories to strings that shouldn't be valid on any platform: you need to set
    // these yourself before you use this config. If you don't, you'll write all over the place as we create 'data' and
    // 'logs' directories willy-nilly. Don't do that.
    Client client = (Client) l1CommonConfig().getBean();
    client.setLogs(BOGUS_FILENAME);

    Server server = (Server) l2CommonConfig().getBean();
    server.setData(BOGUS_FILENAME);
    server.setLogs(BOGUS_FILENAME);
    server.setStatistics(BOGUS_FILENAME);
    server.setStatistics(BOGUS_FILENAME);
  }

  public void activateConfigurationChange() throws ConfigurationSetupException {
    Set allRepositories = collectAllRepositories();

    Iterator iter = allRepositories.iterator();
    while (iter.hasNext()) {
      MutableBeanRepository repository = (MutableBeanRepository) iter.next();
      checkValidates(repository.bean());
      repository.didMutateBean();
    }

    iter = allRepositories.iterator();
    while (iter.hasNext()) {
      MutableBeanRepository repository = (MutableBeanRepository) iter.next();
      repository.saveCopyOfBeanInAnticipationOfFutureMutation();
    }
  }

  private void checkValidates(XmlObject bean) throws ConfigurationSetupException {
    List errors = new ArrayList();
    XmlOptions options = new XmlOptions().setErrorListener(errors);
    boolean valid = bean.validate(options);

    if ((!valid) || (errors.size() > 0)) {
      // formatting
      throw new ConfigurationSetupException("You have errors in your config: " + errors);
    }
  }

  private Set collectAllRepositories() {
    Set allRepositories = new HashSet();
    return allRepositories;
  }

  public SystemConfig systemConfig() {
    return this.sampleSystem;
  }

  public CommonL1Config l1CommonConfig() {
    return this.sampleL1Common;
  }

  public L1DSOConfig l1DSOConfig() {
    return this.sampleL1DSO;
  }

  private void cleanBeanSetServersIfNeeded(Servers l2s) {
    Assert.assertNotNull(l2s);

    if (l2s.sizeOfServerArray() == 1) {
      Server l2 = l2s.getServerArray(0);
      if (l2.getName() != null && l2.getName().equals(DEFAULT_SERVER_NAME) && l2.getHost().equals(DEFAULT_HOST)) {
        l2s.removeServer(0);
        if (l2s.sizeOfServerArray() != 0) { throw new AssertionError("Default server has not been cleared"); }
      }
    }
  }

  private void cleanBeanSetServerGroupsIfNeeded(Servers l2s) {
    Assert.assertNotNull(l2s);

    MirrorGroups groups = l2s.getMirrorGroups();
    Assert.assertNotNull(groups);
    if (groups != null) {
      MirrorGroup[] groupArray = groups.getMirrorGroupArray();

      if (groupArray.length == 1) {
        Members members = groupArray[0].getMembers();
        Assert.assertNotNull(members);
        String[] memberNames = members.getMemberArray();
        Assert.assertNotNull(memberNames);

        if (memberNames.length == 1 && memberNames[0].equals(DEFAULT_SERVER_NAME)) {
          groups.removeMirrorGroup(0);
          Assert.assertEquals(0, groups.getMirrorGroupArray().length);
        }
      }
    }
  }

  // This function will add servers and groups to L1 config
  public void addServersAndGroupsToL1Config(GroupData[] grpData) throws ConfigurationSetupException, XmlException {
    assertIfCalledBefore();
    cleanServersForL1s();
    Servers servers = (Servers) this.sampleL1Manager.serversBeanRepository().bean();

    // One by one add all the servers of a group and then add the group
    for (int i = 0; i < grpData.length; i++) {
      for (int j = 0; j < grpData[i].getServerCount(); j++)
        addServerToL1Config(grpData[i].getServerNames()[j], grpData[i].getDsoPorts()[j], grpData[i].getJmxPorts()[j],
                            false);

      addServerGroupToL1Config(i, grpData[i].getGroupName(), grpData[i].getServerNames());
    }

    TcConfig config = TcConfig.Factory.newInstance();
    config.setServers(servers);
    L2DSOConfigObject.initializeServers(config, this.defaultValueProvider,
                                        this.configurationCreator.directoryConfigurationLoadedFrom());

    setServersBeanForL1s(config.getServers(), "From test froamework");
    isConfigDone = true;
  }

  private void cleanServersForL1s() {
    Servers servers = (Servers) this.sampleL1Manager.serversBeanRepository().bean();
    for (int i = 0; i < servers.getServerArray().length; i++) {
      servers.removeServer(i);
    }

    for (int i = 0; i < servers.getMirrorGroups().getMirrorGroupArray().length; i++) {
      servers.getMirrorGroups().removeMirrorGroup(i);
    }
  }

  private void setServersBeanForL1s(Servers servers, String descp) throws XmlException {
    this.sampleL1Manager.serversBeanRepository().setBean(servers, descp);
  }

  // This function will add all the servers in a group in L1 config. Ideally should be used when only 1 group contains
  // all the servers
  public void addServersAndGroupToL1Config(Servers servers) {
    assertIfCalledBefore();
    Servers l2s = (Servers) this.sampleL1Manager.serversBeanRepository().bean();
    cleanBeanSetServersIfNeeded(l2s);

    l2s.setServerArray(servers.getServerArray());
    l2s.setMirrorGroups(servers.getMirrorGroups());
    isConfigDone = true;
  }

  // This is needed for add-new-stripe test.
  // Allowing a new stripe be added to existing L1 config. Refer DEV-3989.
  public void appendNewServersAndGroupToL1Config(int gn, String groupName, String[] name, int[] dsoPorts, int[] jmxPorts)
      throws ConfigurationSetupException, XmlException {

    for (int i = 0; i < name.length; i++) {
      addServerToL1Config(name[i], dsoPorts[i], jmxPorts[i], false);
    }

    addServerGroupToL1Config(gn, groupName, name);

    TcConfig config = TcConfig.Factory.newInstance();
    Servers servers = (Servers) this.sampleL1Manager.serversBeanRepository().bean();
    config.setServers(servers);
    L2DSOConfigObject.initializeServers(config, this.defaultValueProvider,
                                        this.configurationCreator.directoryConfigurationLoadedFrom());
    setServersBeanForL1s(config.getServers(), "From Test Framework");
  }

  private void assertIfCalledBefore() throws AssertionError {
    if (isConfigDone) throw new AssertionError("Config factory not used properly. Servers were added more than once.");
  }

  public void addServerToL1Config(String bind, int dsoPort, int jmxPort) {
    assertIfCalledBefore();
    Assert.assertTrue(dsoPort >= 0);
    Servers l2s = (Servers) this.sampleL1Manager.serversBeanRepository().bean();
    cleanBeanSetServersIfNeeded(l2s);
    l2s.setServerArray(((Servers) this.sampleL2Manager.serversBeanRepository().bean()).getServerArray());
    l2s.getServerArray(0).getDsoPort().setIntValue(dsoPort);
    if (jmxPort > 0) l2s.getServerArray(0).getJmxPort().setIntValue(jmxPort);

    if (bind != null) {
      l2s.getServerArray(0).setBind(bind);
      l2s.getServerArray(0).getDsoPort().setBind(bind);
      l2s.getServerArray(0).getJmxPort().setBind(bind);
    }
    isConfigDone = true;
  }

  public void addTcPropertyToConfig(String propertyName, String propertyValue) {
    Property tcProps = ((TcProperties) this.sampleL1Manager.tcPropertiesRepository().bean()).addNewProperty();
    tcProps.setName(propertyName);
    tcProps.setValue(propertyValue);
  }

  private void addServerToL1Config(String name, int dsoPort, int jmxPort, boolean cleanGroupsBeanSet) {
    Assert.assertTrue(dsoPort >= 0);
    Servers l2s = (Servers) this.sampleL1Manager.serversBeanRepository().bean();
    cleanBeanSetServersIfNeeded(l2s);
    Server server = l2s.addNewServer();
    server.setName(name);
    server.setHost("%i");
    server.addNewDsoPort().setIntValue(dsoPort);
    server.addNewJmxPort().setIntValue(jmxPort);

    if (cleanGroupsBeanSet) cleanBeanSetServerGroupsIfNeeded(l2s);
  }

  private void addServerGroupToL1Config(int groupId, String groupName, String[] members) {
    Assert.assertNotNull(members);
    Assert.assertTrue(members.length > 0);
    Assert.assertTrue(groupId >= 0);
    Servers l2s = (Servers) this.sampleL1Manager.serversBeanRepository().bean();
    cleanBeanSetServerGroupsIfNeeded(l2s);

    MirrorGroups groups;
    if (!l2s.isSetMirrorGroups()) {
      groups = l2s.addNewMirrorGroups();
    } else {
      groups = l2s.getMirrorGroups();
    }
    if (groups != null) {
      MirrorGroup group = groups.addNewMirrorGroup();
      group.setGroupName(groupName);
      Members newMembers = group.addNewMembers();
      for (String member : members) {
        String memberName = member;
        if (memberName == null || memberName.equals("")) {
          memberName = DEFAULT_HOST;
        }
        newMembers.addMember(member);
      }
    }

  }

  public void setOffHeapConfigObject(boolean enabled, String maxDataSize) {
    offHeapEnabled = enabled;
    maxOffHeapDataSize = maxDataSize;
    Offheap offheap = Offheap.Factory.newInstance();
    offheap.setEnabled(enabled);
    offheap.setMaxDataSize(maxDataSize);
    l2DSOConfig().offHeapConfig().setEnabled(enabled);
    l2DSOConfig().offHeapConfig().setMaxDataSize(maxDataSize);
  }

  public void setGCEnabled(boolean val) {
    gcEnabled = val;
    l2DSOConfig().garbageCollection().setEnabled(val);
  }

  public void setGCVerbose(boolean val) {
    gcVerbose = val;
    l2DSOConfig().garbageCollection().setVerbose(val);
  }

  public void setGCIntervalInSec(int val) {
    gcIntervalInSec = val;
    l2DSOConfig().garbageCollection().setInterval(val);
  }

  public void setPersistenceMode(PersistenceMode.Enum val) {
    persistenceMode = val;
    l2DSOConfig().getPersistence().setMode(val);
  }

  public boolean isOffHeapEnabled() {
    return offHeapEnabled;
  }

  public String getOffHeapMaxDataSize() {
    return maxOffHeapDataSize;
  }

  public boolean getGCEnabled() {
    return gcEnabled;
  }

  public boolean getGCVerbose() {
    return gcVerbose;
  }

  public int getGCIntervalInSec() {
    return gcIntervalInSec;
  }

  public PersistenceMode.Enum getPersistenceMode() {
    return persistenceMode;
  }

  public CommonL2Config l2CommonConfig() {
    return this.sampleL2Common;
  }

  public L2DSOConfig l2DSOConfig() {
    return this.sampleL2DSO;
  }

  public ActiveServerGroupsConfig activeServerGroupsConfig() {
    return this.sampleActiveServerGroups;
  }

  public HaConfigSchema haConfig() {
    return this.sampleHa;
  }

  public TestConfigurationSetupManagerFactory(String l2Identifier,
                                              IllegalConfigurationChangeHandler illegalConfigurationChangeHandler)
      throws ConfigurationSetupException {
    this(MODE_CENTRALIZED_CONFIG, l2Identifier, illegalConfigurationChangeHandler);
  }

  public TestConfigurationSetupManagerFactory(IllegalConfigurationChangeHandler illegalConfigurationChangeHandler)
      throws ConfigurationSetupException {
    this(null, illegalConfigurationChangeHandler);
  }

  public L1ConfigurationSetupManager getL1TVSConfigurationSetupManager() {
    return this.sampleL1Manager;
  }

  public L2ConfigurationSetupManager getL2TVSConfigurationSetupManager() {
    return this.sampleL2Manager;
  }

  public L1ConfigurationSetupManagerImpl createL1TVSConfigurationSetupManager(TestConfigurationCreator configCreator)
      throws ConfigurationSetupException {
    if (mode == MODE_CENTRALIZED_CONFIG) {
      StringBuffer l2sSpec = new StringBuffer();

      String hostname = this.sampleL2DSO.host();
      if (hostname == null) hostname = this.sampleL2DSO.serverName();
      Assert.assertNotBlank(hostname);

      l2sSpec.append(hostname + ":" + this.sampleL2DSO.dsoPort().getIntValue());

      System.setProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME, l2sSpec.toString());
    }

    L1ConfigurationSetupManagerImpl configSetupManager = new L1ConfigurationSetupManagerImpl(configCreator,
                                                                                             this.defaultValueProvider,
                                                                                             this.xmlObjectComparator,
                                                                                             this.illegalChangeHandler);

    return configSetupManager;
  }

  public L2ConfigurationSetupManagerImpl createL2TVSConfigurationSetupManager(String l2Identifier)
      throws ConfigurationSetupException {
    String effectiveL2Identifier = l2Identifier == null ? this.defaultL2Identifier : l2Identifier;
    return new L2ConfigurationSetupManagerImpl(this.configurationCreator, effectiveL2Identifier,
                                               this.defaultValueProvider, this.xmlObjectComparator,
                                               this.illegalChangeHandler);
  }

  // used for just parsing and verifying tc-config.xml
  public L2ConfigurationSetupManager createL2TVSConfigurationSetupManager(File tcConfig, String l2Identifier)
      throws ConfigurationSetupException {
    String effectiveL2Identifier = l2Identifier == null ? this.defaultL2Identifier : l2Identifier;
    ConfigurationCreator confiCreator = new StandardXMLFileConfigurationCreator(
                                                                                new ConfigurationSpec(
                                                                                                      tcConfig
                                                                                                          .getAbsolutePath(),
                                                                                                      ConfigMode.L2,
                                                                                                      tcConfig
                                                                                                          .getParentFile()),
                                                                                this.beanFactory);
    return new L2ConfigurationSetupManagerImpl(confiCreator, effectiveL2Identifier, this.defaultValueProvider,
                                               this.xmlObjectComparator, this.illegalChangeHandler);
  }
}

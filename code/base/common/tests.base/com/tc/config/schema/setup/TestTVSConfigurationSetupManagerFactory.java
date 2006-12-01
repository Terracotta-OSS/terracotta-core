/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.common.XPath;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.NewSystemConfig;
import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.TestConfigObjectInvocationHandler;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewL1DSOConfig;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.util.Assert;
import com.terracottatech.configV2.Application;
import com.terracottatech.configV2.Server;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A {@link com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory} that creates config appropriate for usage in
 * tests. This config behaves just like normal config, except that it reads no files; everything is in-memory instead.
 * You can specify whether you want this config to act like centralized config (all at L2), or distributed config (every
 * L1 has its own copy of the config, too).
 * </p>
 * <p>
 * To use this class, simply get the appropriate config object that you need by calling a method (<em>e.g.</em>,
 * {@link #systemConfig()}). Then, call a method on it (like {@link com.tc.config.schema.NewSystemConfig#dsoEnabled()},
 * for example); this will give you back a {@link ConfigItem}, or a subinterface thereof. Cast this item to a
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
 * {@link ConfigItem}s return. (This is because we're using the real config system &mdash; see below for details
 * &mdash; and it expects {@link XmlObject}s of the appropriate type so it can translate them to the Terracotta-defined
 * types that we really return.) Fortunately, all XML beans have <code>Factory</code> inner classes that will let you
 * create them. If you then wrap these calls in a function and reuse it, you'll be in fine shape if/when the actual XML
 * beans are changed.
 * </p>
 * <p>
 * Note: There is no support yet for different L1s having different config, or config that differs from L2's.
 * </p>
 * <h3>Maintenance:</h3>
 * <p>
 * If you create new typed subinterfaces of {@link ConfigItem}, you do need to make
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler.OurSettableConfigItem} implement them. Don't worry,
 * though; the methods can just throw {@link com.tc.util.TCAssertionError}, and don't need to (nor should they)
 * actually do anything.
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
 * config, the {@link Application} for each application's config, and so on.</li>
 * <li>These {@link XmlObject}s are honest-to-God real instances, as created by their factories (for example,
 * {@link L1.Factory}. At the start, they have just enough configuration populated into them to make sure they
 * validate.</li>
 * <li>This class exposes what look like instances of the normal config objects available to the system. However, these
 * are actually proxies created with {@link java.lang.reflect.Proxy}, using a
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler}.</li>
 * <li>That invocation handler, in response to method calls, parcels out {@link ConfigItem}s that are instances of
 * {@link com.tc.config.schema.TestConfigObjectInvocationHandler.OurSettableConfigItem}. When you call
 * <code>setValue</code> on them, they do their magic: using the {@link XPath} they get from the corresponding
 * "sample" {@link ConfigItem} (see below), they descend the tree of {@link XmlObject}s, starting at the root, creating
 * children along the way as necessary, and finally set the correct property on the correct bean. (This is conceptually
 * easy but actually full of all kinds of nasty mess; this is why {@link OurSettableConfigItem} is such a messy class.) .</li>
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
public class TestTVSConfigurationSetupManagerFactory extends BaseTVSConfigurationSetupManagerFactory {

  public static final int                MODE_CENTRALIZED_CONFIG = 0;
  public static final int                MODE_DISTRIBUTED_CONFIG = 1;

  private final TestConfigBeanSet        beanSet;

  private final TestConfigurationCreator l1ConfigurationCreator;
  private final TestConfigurationCreator l2ConfigurationCreator;

  private final NewSystemConfig          sampleSystem;
  private final NewCommonL1Config        sampleL1Common;
  private final NewL1DSOConfig           sampleL1DSO;
  private final NewCommonL2Config        sampleL2Common;
  private final NewL2DSOConfig           sampleL2DSO;
  private final NewDSOApplicationConfig  sampleDSOApplication;

  private final String                   defaultL2Identifier;

  private final int                      mode;

  public TestTVSConfigurationSetupManagerFactory(int mode, String l2Identifier,
                                                 IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    super(illegalConfigurationChangeHandler);

    this.beanSet = new TestConfigBeanSet();

    this.l2ConfigurationCreator = new TestConfigurationCreator(this.beanSet, true);

    this.mode = mode;
    if (mode == MODE_CENTRALIZED_CONFIG) {
      this.l1ConfigurationCreator = new TestConfigurationCreator(this.beanSet, true);
    } else if (mode == MODE_DISTRIBUTED_CONFIG) {
      this.l1ConfigurationCreator = new TestConfigurationCreator(this.beanSet, false);
    } else {
      throw Assert.failure("Unknown mode: " + mode);
    }

    if (l2Identifier != null) {
      Assert.assertNotBlank(l2Identifier);
      this.defaultL2Identifier = l2Identifier;
    } else {
      this.defaultL2Identifier = this.beanSet.serversBean().getServerArray()[0].getName();
    }

    Assert.assertNotNull(this.defaultL2Identifier);

    // FIXME 2005-11-30 andrew -- This stinks like mad...we should be able to do something better than perverting the
    // existing config-setup managers here.
    L1TVSConfigurationSetupManager sampleL1Manager;
    L2TVSConfigurationSetupManager sampleL2Manager;

    try {
      sampleL1Manager = this.createL1TVSConfigurationSetupManager(new TestConfigurationCreator(this.beanSet, true));
      sampleL2Manager = this.createL2TVSConfigurationSetupManager(null);
    } catch (ConfigurationSetupException cse) {
      throw Assert.failure("Huh?", cse);
    }

    this.sampleSystem = sampleL2Manager.systemConfig();
    this.sampleL1Common = sampleL1Manager.commonL1Config();
    this.sampleL1DSO = sampleL1Manager.dsoL1Config();
    this.sampleL2Common = sampleL2Manager.commonl2Config();
    this.sampleL2DSO = sampleL2Manager.dsoL2Config();
    this.sampleDSOApplication = sampleL1Manager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);

    applyDefaultTestConfig();
  }

  public TestConfigBeanSet beanSet() {
    return this.beanSet;
  }

  private static final String BOGUS_FILENAME = "nonexistent-directory-SHOULD-NEVER-EXIST/../";

  private void applyDefaultTestConfig() {
//    // Use a license that lets us do anything.
//    try {
//      String path = getEverythingLicensePath();
//      ((SettableConfigItem) systemConfig().licenseLocation()).setValue(path);
//    } catch (IOException ioe) {
//      throw Assert.failure("Unable to fetch data directory root to find license for tests.", ioe);
//    }
//
//    ((SettableConfigItem) systemConfig().licenseType()).setValue(LicenseType.PRODUCTION);
//
//    // Make servers use dynamic ports, by default.
//    ((SettableConfigItem) l2DSOConfig().listenPort()).setValue(0);
    ((SettableConfigItem) l2CommonConfig().jmxPort()).setValue(0);

    // We also set the data and log directories to strings that shouldn't be valid on any platform: you need to set
    // these yourself before you use this config. If you don't, you'll write all over the place as we create 'data' and
    // 'logs' directories willy-nilly. Don't do that.
    ((SettableConfigItem) l1CommonConfig().logsPath()).setValue(BOGUS_FILENAME);
    ((SettableConfigItem) l2CommonConfig().dataPath()).setValue(BOGUS_FILENAME);
    ((SettableConfigItem) l2CommonConfig().logsPath()).setValue(BOGUS_FILENAME);
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

    allRepositories.addAll(Arrays.asList(this.l1ConfigurationCreator.allRepositoriesStoredInto()));
    allRepositories.addAll(Arrays.asList(this.l2ConfigurationCreator.allRepositoriesStoredInto()));
    return allRepositories;
  }

  private Object proxify(Class theClass, XmlObject[] destObjects, Object realImplementation, String xpathPrefix) {
    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { theClass },
                                  new TestConfigObjectInvocationHandler(theClass, destObjects, realImplementation,
                                                                        xpathPrefix));
  }

  private Object proxify(Class theClass, XmlObject destObject, Object realImplementation, String xpathPrefix) {
    return proxify(theClass, new XmlObject[] { destObject }, realImplementation, xpathPrefix);
  }

  private XmlObject[] allServerBeans() {
    return this.beanSet.serversBean().getServerArray();
  }

  public NewSystemConfig systemConfig() {
    return (NewSystemConfig) proxify(NewSystemConfig.class, this.beanSet.systemBean(), this.sampleSystem, null);
  }

  public NewCommonL1Config l1CommonConfig() {
    return (NewCommonL1Config) proxify(NewCommonL1Config.class, this.beanSet.clientBean(), this.sampleL1Common, null);
  }

  public NewL1DSOConfig l1DSOConfig() {
    return (NewL1DSOConfig) proxify(NewL1DSOConfig.class, this.beanSet.clientBean(), this.sampleL1DSO, "dso");
  }

  public void addServerConfig(String name) {
    Server newL2 = this.beanSet.serversBean().addNewServer();

    newL2.setName(name);
    newL2.setHost("localhost");

    newL2.setDsoPort(0);
    newL2.setJmxPort(0);

    newL2.setData(BOGUS_FILENAME);
    newL2.setLogs(BOGUS_FILENAME);
  }

  private Server findL2Bean(String name) {
    Server[] allServers = this.beanSet.serversBean().getServerArray();

    if (allServers == null || allServers.length == 0) throw Assert.failure("No L2s are defined.");

    if (name == null) {
      if (allServers.length == 1) return allServers[0];
      else throw Assert
          .failure("You passed in null for the L2 name, but there's more than one L2. Please specify which one you want.");
    }

    for (int i = 0; i < allServers.length; ++i) {
      if (allServers[i].getName().equals(name)) return allServers[i];
    }
    throw Assert.failure("There is no L2 defined named '" + name + "'.");
  }

  public NewCommonL2Config l2CommonConfig(String l2Name) {
    return (NewCommonL2Config) proxify(NewCommonL2Config.class, findL2Bean(l2Name), this.sampleL2Common, null);
  }

  public NewL2DSOConfig l2DSOConfig(String l2Name) {
    return (NewL2DSOConfig) proxify(NewL2DSOConfig.class, findL2Bean(l2Name), this.sampleL2DSO, null);
  }

  public NewCommonL2Config l2CommonConfig() {
    return (NewCommonL2Config) proxify(NewCommonL2Config.class, allServerBeans(), this.sampleL2Common, null);
  }

  public NewL2DSOConfig l2DSOConfig() {
    return (NewL2DSOConfig) proxify(NewL2DSOConfig.class, allServerBeans(), this.sampleL2DSO, null);
  }

  public NewDSOApplicationConfig dsoApplicationConfig(String applicationName) {
    return (NewDSOApplicationConfig) proxify(NewDSOApplicationConfig.class, this.beanSet
        .applicationBeanFor(applicationName), this.sampleDSOApplication, "dso");
  }

  public NewDSOApplicationConfig dsoApplicationConfig() {
    return dsoApplicationConfig(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);
  }

  public TestTVSConfigurationSetupManagerFactory(String l2Identifier,
                                                 IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    this(MODE_CENTRALIZED_CONFIG, l2Identifier, illegalConfigurationChangeHandler);
  }

  public TestTVSConfigurationSetupManagerFactory(IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    this(null, illegalConfigurationChangeHandler);
  }

  public L1TVSConfigurationSetupManager createL1TVSConfigurationSetupManager() throws ConfigurationSetupException {
    return createL1TVSConfigurationSetupManager(this.l1ConfigurationCreator);
  }

  public L1TVSConfigurationSetupManager createL1TVSConfigurationSetupManager(TestConfigurationCreator configCreator)
      throws ConfigurationSetupException {
    if (mode == MODE_CENTRALIZED_CONFIG) {
      StringBuffer l2sSpec = new StringBuffer();

      Server[] allServers = (Server[]) this.allServerBeans();
      for (int i = 0; i < allServers.length; ++i) {
        Server thisServer = allServers[i];

        if (i > 0) l2sSpec.append(",");

        String hostname = thisServer.getHost();
        if (hostname == null) hostname = thisServer.getName();
        Assert.assertNotBlank(hostname);

        l2sSpec.append(hostname + ":" + thisServer.getDsoPort());
      }

      System.setProperty(TVSConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME, l2sSpec.toString());
    }

    return new StandardL1TVSConfigurationSetupManager(configCreator, this.defaultValueProvider,
                                                      this.xmlObjectComparator, this.illegalChangeHandler);
  }

  public L2TVSConfigurationSetupManager createL2TVSConfigurationSetupManager(String l2Identifier)
      throws ConfigurationSetupException {
    String effectiveL2Identifier = l2Identifier == null ? this.defaultL2Identifier : l2Identifier;
    return new StandardL2TVSConfigurationSetupManager(this.l2ConfigurationCreator, effectiveL2Identifier,
                                                      this.defaultValueProvider, this.xmlObjectComparator,
                                                      this.illegalChangeHandler);
  }

}

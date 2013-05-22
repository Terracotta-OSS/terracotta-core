/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.RootLogger;

import com.tc.client.AbstractClientFactory;
import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.platform.PlatformService;
import com.tc.util.Assert;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;

public class DSOContextImpl implements DSOContext {
  private static final TCLogger          logger = TCLogging.getLogger(DSOContextImpl.class);

  private final ManagerImpl              manager;
  private final String                   configSpec;
  private final TCSecurityManager        securityManager;
  private final SecurityInfo             securityInfo;

  private volatile DSOClientConfigHelper configHelper;

  public static DSOContext createStandaloneContext(String configSpec, ClassLoader loader, boolean expressRejoinClient,
                                                   TCSecurityManager securityManager, SecurityInfo securityInfo) {
    ManagerImpl manager = new ManagerImpl(true, null, null, null, null, null, null, true, loader, expressRejoinClient,
                                          securityManager);
    DSOContextImpl context = createContext(manager, configSpec, securityManager, securityInfo);
    return context;
  }

  public void init() throws ConfigurationSetupException {
    StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    (String[]) null,
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.EXPRESS_L1,
                                                                                                    new FatalIllegalConfigurationChangeHandler(),
                                                                                                    configSpec,
                                                                                                    securityManager);

    L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager(securityInfo);
    config.setupLogging();
    PreparedComponentsFromL2Connection l2Connection;
    try {
      l2Connection = validateMakeL2Connection(config, securityManager);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    DSOClientConfigHelper configHelperLocal = new StandardDSOClientConfigHelperImpl(config);

    this.configHelper = configHelperLocal;
    manager.set(configHelper, l2Connection);

    try {
      startToolkitConfigurator();
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }
    manager.init();
  }

  public PlatformService getPlatformService() {
    return manager.getPlatformService();
  }

  public static TCSecurityManager createSecurityManager(Map<String, Object> env) {
    return AbstractClientFactory.getFactory().createClientSecurityManager(env);
  }

  private void startToolkitConfigurator() throws Exception {
    Class toolkitConfiguratorClass = null;
    try {
      toolkitConfiguratorClass = Class.forName("com.terracotta.toolkit.EnterpriseToolkitConfigurator");
    } catch (ClassNotFoundException e) {
      toolkitConfiguratorClass = Class.forName("com.terracotta.toolkit.ToolkitConfigurator");
    }

    Object toolkitConfigurator = toolkitConfiguratorClass.newInstance();
    Method start = toolkitConfiguratorClass.getMethod("start", DSOClientConfigHelper.class);
    start.invoke(toolkitConfigurator, configHelper);
  }

  private static DSOContextImpl createContext(ManagerImpl manager, String configSpec,
                                              TCSecurityManager securityManager, SecurityInfo securityInfo) {
    return new DSOContextImpl(manager.getClassProvider(), manager, configSpec, securityManager, securityInfo);
  }

  private DSOContextImpl(ClassProvider classProvider, ManagerImpl manager, String configSpec,
                         TCSecurityManager securityManager, SecurityInfo securityInfo) {
    resolveClasses();

    this.manager = manager;
    this.configSpec = configSpec;
    this.securityManager = securityManager;
    this.securityInfo = securityInfo;
  }

  private void resolveClasses() {
    // This is to help a deadlock in log4j (see MNK-3461, MNK-3512)
    Logger l = new RootLogger(Level.ALL);
    Hierarchy h = new Hierarchy(l);
    l.addAppender(new WriterAppender(new PatternLayout(TCLogging.FILE_AND_JMX_PATTERN), new OutputStream() {
      @Override
      public void write(int b) {
        //
      }
    }));
    l.debug(h.toString(), new Throwable());
  }

  @Override
  public Manager getManager() {
    return this.manager;
  }

  @Override
  public void addTunneledMBeanDomain(String mbeanDomain) {
    this.configHelper.addTunneledMBeanDomain(mbeanDomain);
  }

  private static PreparedComponentsFromL2Connection validateMakeL2Connection(L1ConfigurationSetupManager config,
                                                                             final TCSecurityManager securityManager) {
    L2Data[] l2Data = config.l2Config().l2Data();
    Assert.assertNotNull(l2Data);

    return new PreparedComponentsFromL2Connection(config, securityManager);
  }

  @Override
  public void shutdown() {
    manager.stop();
  }
}

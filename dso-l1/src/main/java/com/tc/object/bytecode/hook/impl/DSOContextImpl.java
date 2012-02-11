/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import org.apache.commons.io.CopyUtils;

import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.WeavingStrategy;
import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.bundles.Repository;
import com.tc.bundles.VirtualTimRepository;
import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ModuleConfiguration;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.RuntimeLoggerImpl;
import com.tc.plugins.ModulesLoader;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.Util;
import com.terracottatech.config.ConfigurationModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DSOContextImpl implements DSOContext {

  private static final TCLogger       logger        = TCLogging.getLogger(DSOContextImpl.class);
  private static final TCLogger       consoleLogger = CustomerLogging.getConsoleLogger();

  private final DSOClientConfigHelper configHelper;
  private final Manager               manager;
  private final InstrumentationLogger instrumentationLogger;
  private final WeavingStrategy       weavingStrategy;

  private final EmbeddedOSGiRuntime   osgiRuntime;
  private final boolean               expressRejoinClient;

  public static DSOContext createContext(String configSpec) throws ConfigurationSetupException {
    StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    (String[]) null,
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.CUSTOM_L1,
                                                                                                    new FatalIllegalConfigurationChangeHandler(),
                                                                                                    configSpec);

    L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager();
    config.setupLogging();
    PreparedComponentsFromL2Connection l2Connection;
    try {
      l2Connection = validateMakeL2Connection(config);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(config);
    Manager manager = new ManagerImpl(configHelper, l2Connection);
    DSOContext context = createContext(configHelper, manager);
    manager.init();
    return context;
  }

  public static DSOContext createStandaloneContext(String configSpec, ClassLoader loader,
                                                   Map<String, URL> virtualTimJars, boolean expressRejoinClient)
      throws ConfigurationSetupException {
    // XXX: refactor this method to not duplicate createContext() so much

    StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    (String[]) null,
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.EXPRESS_L1,
                                                                                                    new FatalIllegalConfigurationChangeHandler(),
                                                                                                    configSpec);

    L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager();
    config.setupLogging();
    PreparedComponentsFromL2Connection l2Connection;
    try {
      l2Connection = validateMakeL2Connection(config);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(config);
    RuntimeLoggerImpl runtimeLogger = new RuntimeLoggerImpl(configHelper);

    Manager manager = new ManagerImpl(true, null, null, null, null, configHelper, l2Connection, true, runtimeLogger,
                                      loader, expressRejoinClient);

    Collection<Repository> repos = new ArrayList<Repository>();
    repos.add(new VirtualTimRepository(virtualTimJars));
    DSOContextImpl context = createContext(configHelper, manager, repos, expressRejoinClient);
    try {
      context.installBundles(virtualTimJars.values());
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }
    manager.init();

    return context;
  }

  private void installBundles(Collection<URL> bundleURLs) throws Exception {
    ModulesLoader.installAndStartBundles(osgiRuntime, configHelper, manager.getClassProvider(), false,
                                         bundleURLs.toArray(new URL[] {}));
  }

  public static DSOContext createContext(DSOClientConfigHelper configHelper, Manager manager) {
    return createContext(configHelper, manager, Collections.EMPTY_LIST, false);
  }

  private static DSOContextImpl createContext(DSOClientConfigHelper configHelper, Manager manager,
                                              Collection<Repository> repos, boolean expressRejoinClient) {
    return new DSOContextImpl(configHelper, manager.getClassProvider(), manager, repos, expressRejoinClient);
  }

  private DSOContextImpl(DSOClientConfigHelper configHelper, ClassProvider classProvider, Manager manager,
                         Collection<Repository> repos, boolean expressRejoinClient) {
    Assert.assertNotNull(configHelper);

    resolveClasses();

    this.expressRejoinClient = expressRejoinClient;
    this.configHelper = configHelper;
    this.manager = manager;
    this.instrumentationLogger = manager.getInstrumentationLogger();
    this.weavingStrategy = new DefaultWeavingStrategy(configHelper, instrumentationLogger);

    try {
      osgiRuntime = ModulesLoader.initModules(configHelper, classProvider, false, repos);
    } catch (Exception e) {
      consoleLogger.fatal(e.getMessage());
      logger.fatal(e);
      System.exit(1);
      throw new AssertionError("Will not run");
    }
  }

  private void resolveClasses() {
    // This fixes a class circularity error in JavaClassInfoRepository
    JavaClassInfo.getClassInfo(getClass());
  }

  public Manager getManager() {
    return this.manager;
  }

  @Override
  public ModuleConfiguration getModuleConfigurtion() {
    return configHelper;
  }

  /**
   * XXX::NOTE:: ClassLoader checks the returned byte array to see if the class is instrumented or not to maintain the
   * offset.
   * 
   * @return new byte array if the class is instrumented and same input byte array if not.
   * @see ClassLoaderPreProcessorImpl
   */
  public byte[] preProcess(String name, byte[] data, int offset, int length, ClassLoader caller) {
    InstrumentationContext context = new InstrumentationContext(name, data, caller);
    weavingStrategy.transform(name, context);
    return context.getCurrentBytecode();
  }

  public void postProcess(Class clazz, ClassLoader caller) {
    // NOP
  }

  private static PreparedComponentsFromL2Connection validateMakeL2Connection(L1ConfigurationSetupManager config)
      throws UnknownHostException, IOException, TCTimeoutException {
    L2Data[] l2Data = config.l2Config().l2Data();
    Assert.assertNotNull(l2Data);

    if (false && !config.loadedFromTrustedSource()) {
      String serverConfigMode = getServerConfigMode(l2Data[0].host(), l2Data[0].dsoPort());

      if (serverConfigMode != null && serverConfigMode.equals(ConfigurationModel.PRODUCTION)) {
        String text = "Configuration constraint violation: "
                      + "untrusted client configuration not allowed against production server";
        throw new AssertionError(text);
      }
    }

    return new PreparedComponentsFromL2Connection(config);
  }

  private static final long MAX_HTTP_FETCH_TIME       = 30 * 1000; // 30 seconds
  private static final long HTTP_FETCH_RETRY_INTERVAL = 1 * 1000; // 1 second

  private static String getServerConfigMode(String serverHost, int httpPort) throws MalformedURLException,
      TCTimeoutException, IOException {
    URL theURL = new URL("http", serverHost, httpPort, "/config?query=mode");
    long startTime = System.currentTimeMillis();
    long lastTrial = 0;

    boolean interrupted = false;
    try {
      while (System.currentTimeMillis() < (startTime + MAX_HTTP_FETCH_TIME)) {
        try {
          long untilNextTrial = HTTP_FETCH_RETRY_INTERVAL - (System.currentTimeMillis() - lastTrial);

          if (untilNextTrial > 0) {
            try {
              Thread.sleep(untilNextTrial);
            } catch (InterruptedException ie) {
              interrupted = true;
            }
          }

          logger.debug("Opening connection to: " + theURL + " to fetch server configuration.");

          lastTrial = System.currentTimeMillis();
          InputStream in = theURL.openStream();
          logger.debug("Got input stream to: " + theURL);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();

          CopyUtils.copy(in, baos);

          return baos.toString();
        } catch (ConnectException ce) {
          logger.warn("Unable to fetch configuration mode from L2 at '" + theURL + "'; trying again. "
                      + "(Is an L2 running at that address?): " + ce.getLocalizedMessage());
          // oops -- try again
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(interrupted);
    }

    throw new TCTimeoutException("We tried for " + (int) ((System.currentTimeMillis() - startTime) / 1000)
                                 + " seconds, but couldn't fetch system configuration mode from the L2 " + "at '"
                                 + theURL + "'. Is the L2 running?");
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    return preProcess(className, classfileBuffer, 0, classfileBuffer.length, loader);
  }

  public void shutdown() {
    osgiRuntime.shutdown();
    if (expressRejoinClient) {
      manager.stopImmediate();
    } else {
      manager.stop();
    }
  }
}

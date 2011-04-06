/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import static org.terracotta.license.LicenseConstants.LICENSE_KEY_FILENAME;

import org.apache.commons.io.CopyUtils;

import com.google.common.collect.MapMaker;
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
import com.tc.license.LicenseManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.bytecode.ManagerInternal;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.config.UnverifiedBootJarException;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.SingleLoaderClassProvider;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.RuntimeLoggerImpl;
import com.tc.object.tools.BootJar;
import com.tc.object.tools.BootJarException;
import com.tc.plugins.ModulesLoader;
import com.tc.timapi.Version;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
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
import java.util.HashMap;
import java.util.Map;

public class DSOContextImpl implements DSOContext {

  private static final TCLogger                     logger                 = TCLogging.getLogger(DSOContextImpl.class);
  private static final TCLogger                     consoleLogger          = CustomerLogging.getConsoleLogger();

  private static DSOClientConfigHelper              staticConfigHelper;
  private static PreparedComponentsFromL2Connection preparedComponentsFromL2Connection;

  private final DSOClientConfigHelper               configHelper;
  private final Manager                             manager;
  private final InstrumentationLogger               instrumentationLogger;
  private final WeavingStrategy                     weavingStrategy;

  private final static String                       UNVERIFIED_BOOTJAR_MSG = "\n********************************************************************************\n"
                                                                             + "There is a mismatch between the expected Terracotta boot JAR file and the\n"
                                                                             + "existing Terracotta boot JAR file. Recreate the boot JAR file using the\n"
                                                                             + "following command from the Terracotta home directory:\n"
                                                                             + "\n"
                                                                             + "platform/bin/make-boot-jar.sh -f <path/to/Terracotta/configuration/file>\n"
                                                                             + "\n"
                                                                             + "or\n"
                                                                             + "\n"
                                                                             + "platform/bin\\make-boot-jar.bat -f <path\\to\\Terracotta\\configuration\\file>\n"
                                                                             + "\n"
                                                                             + "Enter the make-boot-jar command with the -h switch for help.\n"
                                                                             + "********************************************************************************\n";
  private final EmbeddedOSGiRuntime                 osgiRuntime;
  private final boolean                             immediateStop;

  /**
   * Creates a "global" DSO Context. This context is appropriate only when there is only one DSO Context that applies to
   * the entire VM
   */
  public static DSOContext createGlobalContext() throws ConfigurationSetupException {
    DSOClientConfigHelper configHelper = getGlobalConfigHelper();
    Manager manager = new ManagerImpl(configHelper, preparedComponentsFromL2Connection);
    return new DSOContextImpl(configHelper, manager.getClassProvider(), manager, Collections.EMPTY_LIST, false);
  }

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
                                                   Map<String, URL> virtualTimJars, Collection<URL> additionalModules,
                                                   URL bootJarURL, boolean immediateStop)
      throws ConfigurationSetupException {
    // XXX: refactor this method to not duplicate createContext() so much

    // load license via normal methods before attempt to load it from application resource
    if (LicenseManager.getLicense() == null) {
      String licenseLocation = "/" + LICENSE_KEY_FILENAME;
      LicenseManager.loadLicenseFromStream(loader.getResourceAsStream(licenseLocation), licenseLocation);
    }

    try {
      BootJar.verifyTCVersion(bootJarURL);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

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

    boolean HAS_BOOT_JAR = false;

    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(config, HAS_BOOT_JAR);
    RuntimeLoggerImpl runtimeLogger = new RuntimeLoggerImpl(configHelper);
    // XXX: what should the appGroup and loaderDesc be? In theory we might want "regular" clients to access this shared
    // state too
    ClassProvider classProvider = new SingleLoaderClassProvider(null, "standalone", loader);
    Manager manager = new ManagerImpl(true, null, null, null, null, configHelper, l2Connection, true, runtimeLogger,
                                      classProvider, true);

    Collection<Repository> repos = new ArrayList<Repository>();
    repos.add(new VirtualTimRepository(virtualTimJars));
    DSOContext context = createContext(configHelper, manager, repos, immediateStop);
    if (additionalModules != null && !additionalModules.isEmpty()) {
      try {
        context.addModules(additionalModules.toArray(new URL[0]));
      } catch (Exception e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
    }
    manager.init();

    return context;
  }

  /**
   * For tests
   */

  public static DSOContext createContext(DSOClientConfigHelper configHelper, Manager manager) {
    return createContext(configHelper, manager, Collections.EMPTY_LIST, false);
  }

  public static DSOContext createContext(DSOClientConfigHelper configHelper, Manager manager,
                                         Collection<Repository> repos, boolean immediateStop) {
    return new DSOContextImpl(configHelper, manager.getClassProvider(), manager, repos, immediateStop);
  }

  private DSOContextImpl(DSOClientConfigHelper configHelper, ClassProvider classProvider, Manager manager,
                         Collection<Repository> repos, boolean immediateStop) {
    Assert.assertNotNull(configHelper);

    resolveClasses();

    this.immediateStop = immediateStop;
    this.configHelper = configHelper;
    this.manager = manager;
    this.instrumentationLogger = manager.getInstrumentationLogger();
    this.weavingStrategy = new DefaultWeavingStrategy(configHelper, instrumentationLogger);

    checkForProperlyInstrumentedBaseClasses();

    validateTimApiVersion();

    try {
      osgiRuntime = ModulesLoader.initModules(configHelper, classProvider, manager.getTunneledDomainUpdater(), false,
                                              repos);
      configHelper.validateSessionConfig();
      validateBootJar();
    } catch (Exception e) {
      consoleLogger.fatal(e.getMessage());
      logger.fatal(e);
      System.exit(1);
      throw new AssertionError("Will not run");
    }

    // do a pre-emptive class load since this path gets nested inside other classloads...
    if (configHelper instanceof StandardDSOClientConfigHelper) {
      try {
        ((StandardDSOClientConfigHelper) configHelper).addClassResource("non.existent.Class",
                                                                        new URL("file:///not/a/real/file"), false);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    getClassResource("non.existent.Class", getClass().getClassLoader(), true);
  }

  private void resolveClasses() {
    // This fixes a class circularity error in JavaClassInfoRepository
    new MapMaker().weakKeys().weakValues().makeMap().put("foo", "bar");
  }

  /**
   * Verify that we're not using a SNAPSHOT tim-api with a non-SNAPSHOT core TC
   */
  private void validateTimApiVersion() {
    Version timApiVersion = Version.getVersion();
    ProductInfo info = ProductInfo.getInstance();

    if (timApiVersion.isSnapshot()) {
      if (!info.isDevMode() && !info.version().contains("SNAPSHOT")) {
        //
        throw new AssertionError("Snapshot version of the TIM API (" + timApiVersion.getFullVersionString()
                                 + ") is not permitted with a non-SNAPSHOT core TC version (" + info.version() + ")");
      }
    }
  }

  private void validateBootJar() throws BootJarException {
    if (!configHelper.hasBootJar()) { return; }

    try {
      configHelper.verifyBootJarContents(null);
    } catch (final UnverifiedBootJarException e) {
      StringBuilder msg = new StringBuilder(UNVERIFIED_BOOTJAR_MSG);
      msg.append(e.getMessage() + " ");
      msg.append("Unable to verify the contents of the boot jar; ");
      msg.append("Please check the client logs for more information.");
      throw new BootJarException(msg.toString(), e);
    }
  }

  private void checkForProperlyInstrumentedBaseClasses() {
    if (!configHelper.hasBootJar()) { return; }

    if (!Manageable.class.isAssignableFrom(HashMap.class)) {
      StringBuffer msg = new StringBuffer();
      msg.append("The DSO boot jar is not prepended to your bootclasspath! ");
      msg.append("Generate it using the make-boot-jar script ");
      msg.append("and place the generated jar file in the bootclasspath ");
      msg.append("(i.e. -Xbootclasspath/p:/path/to/terracotta/lib/dso-boot/dso-boot-xxx.jar)");
      throw new Error(msg.toString());
    }
  }

  public Manager getManager() {
    return this.manager;
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

  private synchronized static DSOClientConfigHelper getGlobalConfigHelper() throws ConfigurationSetupException {
    if (staticConfigHelper == null) {
      StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                      StandardConfigurationSetupManagerFactory.ConfigMode.CUSTOM_L1,
                                                                                                      new FatalIllegalConfigurationChangeHandler());

      logger.debug("Created StandardTVSConfigurationSetupManagerFactory.");
      L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager();
      config.setupLogging();
      logger.debug("Created L1TVSConfigurationSetupManager.");

      try {
        preparedComponentsFromL2Connection = validateMakeL2Connection(config);
      } catch (Exception e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
      staticConfigHelper = new StandardDSOClientConfigHelperImpl(config);
    }

    return staticConfigHelper;
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

  public URL getClassResource(String className, ClassLoader loader, boolean hideSystemResources) {
    return configHelper.getClassResource(className, loader, hideSystemResources);
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    return preProcess(className, classfileBuffer, 0, classfileBuffer.length, loader);
  }

  public void addModules(URL[] modules) throws Exception {
    ModulesLoader.installAndStartBundles(osgiRuntime, configHelper, manager.getClassProvider(), manager
        .getTunneledDomainUpdater(), false, modules);
  }

  public void shutdown() {
    osgiRuntime.shutdown();
    if (immediateStop) {
      ((ManagerInternal) manager).stopImmediate();
    } else {
      manager.stop();
    }
  }
}

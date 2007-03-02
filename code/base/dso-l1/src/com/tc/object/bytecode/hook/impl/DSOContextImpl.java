/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import org.apache.commons.io.CopyUtils;

import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.WeavingStrategy;
import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.IncompleteBootJarException;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.UnverifiedBootJarException;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.InstrumentationLoggerImpl;
import com.tc.plugins.ModulesLoader;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.terracottatech.config.ConfigurationModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;

public class DSOContextImpl implements DSOContext {

  private static final TCLogger                     logger = TCLogging.getLogger(DSOContextImpl.class);

  private static DSOClientConfigHelper              staticConfigHelper;
  private static PreparedComponentsFromL2Connection preparedComponentsFromL2Connection;

  private final DSOClientConfigHelper               configHelper;
  private final Manager                             manager;
  private final WeavingStrategy                     weavingStrategy;

  /**
   * Creates a "global" DSO Context. This context is appropriate only when there is only one DSO Context that applies to
   * the entire VM
   */
  public static DSOContext createGlobalContext(ClassProvider globalProvider) throws ConfigurationSetupException {
    DSOClientConfigHelper configHelper = getGlobalConfigHelper();
    Manager manager = new ManagerImpl(configHelper, globalProvider, preparedComponentsFromL2Connection);
    return new DSOContextImpl(configHelper, manager);
  }

  /**
   * For tests
   */
  public static DSOContext createContext(DSOClientConfigHelper configHelper, Manager manager) {
    return new DSOContextImpl(configHelper, manager);
  }

  public static boolean isDSOSessions(String appName) throws ConfigurationSetupException {
    return getGlobalConfigHelper().isDSOSessions(appName);
  }

  private DSOContextImpl(DSOClientConfigHelper configHelper, Manager manager) {
    checkForProperlyInstrumentedBaseClasses();
    if (configHelper == null) { throw new NullPointerException(); }

    this.configHelper = configHelper;
    this.manager = manager;
    weavingStrategy = new DefaultWeavingStrategy(configHelper, new InstrumentationLoggerImpl(configHelper
        .instrumentationLoggingOptions()));

    ModulesLoader.initPlugins(configHelper, false);
    // TODO: This could result in a ConcurrentModificationException in the test framework when one thread
    // finish the validation and continues to instrument user classes which may create new TransparencyClassSpec
    // while another thread is still in the process of validating the boot jar.
    // validateBootJar();
  }

  private void validateBootJar() {
    try {
      configHelper.verifyBootJarContents();
    } catch (final UnverifiedBootJarException ubjex) {
      final StringBuffer msg = new StringBuffer(ubjex.getMessage() + " ");
      msg.append("Unable to verify the contents of the boot jar; ");
      msg.append("Please check the client logs for more information.");
      logger.error(ubjex);
      throw new RuntimeException(msg.toString());
    } catch (final IncompleteBootJarException ibjex) {
      final StringBuffer msg = new StringBuffer(ibjex.getMessage() + " ");
      msg.append("The DSO boot jar appears to be incomplete --- some pre-instrumented classes ");
      msg.append("listed in your tc-config is not included in the boot jar file. This could ");
      msg.append("happen if you've modified your DSO clients' tc-config file to specify additional ");
      msg.append("classes for inclusion in the boot jar, but forgot to rebuild the boot jar. Or, you ");
      msg.append("could be a using an older boot jar against a newer Terracotta client installation. ");
      msg.append("Please check the client logs for the list of classes that were not found in your boot jar.");
      logger.error(ibjex);
      throw new RuntimeException(msg.toString());
    }
  }

  private void checkForProperlyInstrumentedBaseClasses() {
    if (!Manageable.class.isAssignableFrom(HashMap.class)) {
      StringBuffer msg = new StringBuffer();
      msg.append("The DSO boot jar is not prepended to your bootclasspath! ");
      msg.append("Generate it using the make-boot-jar script ");
      msg.append("and place the generated jar file in the bootclasspath ");
      msg.append("(i.e. -Xbootclasspath/p:/path/to/terracotta/lib/dso-boot/dso-boot-xxx.jar)");
      throw new RuntimeException(msg.toString());
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

  // Needed by Spring
  public void addTransient(String className, String fieldName) {
    this.configHelper.addTransient(className, fieldName);
  }

  // Needed by Spring
  public void addInclude(String expression, boolean callConstructorOnLoad, String lockExpression) {
    this.configHelper.addIncludeAndLockIfRequired(expression, true, callConstructorOnLoad, false, lockExpression);
  }

  // Needed by Spring
  public Collection getDSOSpringConfigHelpers() {
    return this.configHelper.getDSOSpringConfigs();
  }

  private synchronized static DSOClientConfigHelper getGlobalConfigHelper() throws ConfigurationSetupException {
    if (staticConfigHelper == null) {
      StandardTVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(
                                                                                                            false,
                                                                                                            new FatalIllegalConfigurationChangeHandler());

      logger.debug("Created StandardTVSConfigurationSetupManagerFactory.");
      L1TVSConfigurationSetupManager config = factory.createL1TVSConfigurationSetupManager();
      config.setupLogging();
      logger.debug("Created L1TVSConfigurationSetupManager.");

      try {
        preparedComponentsFromL2Connection = validateMakeL2Connection(config);
      } catch (Exception e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
      staticConfigHelper = new StandardDSOClientConfigHelper(config);
    }

    return staticConfigHelper;
  }

  private static PreparedComponentsFromL2Connection validateMakeL2Connection(L1TVSConfigurationSetupManager config)
      throws UnknownHostException, IOException, TCTimeoutException {
    L2Data[] l2Data = (L2Data[]) config.l2Config().l2Data().getObjects();
    Assert.assertNotNull(l2Data);

    String serverHost = l2Data[0].host();

    if (false && !config.loadedFromTrustedSource()) {
      String serverConfigMode = getServerConfigMode(serverHost, l2Data[0].dsoPort());

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

    while (System.currentTimeMillis() < (startTime + MAX_HTTP_FETCH_TIME)) {
      try {
        long untilNextTrial = HTTP_FETCH_RETRY_INTERVAL - (System.currentTimeMillis() - lastTrial);

        if (untilNextTrial > 0) {
          try {
            Thread.sleep(untilNextTrial);
          } catch (InterruptedException ie) {
            // whatever; just try again now
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

    throw new TCTimeoutException("We tried for " + (int) ((System.currentTimeMillis() - startTime) / 1000)
                                 + " seconds, but couldn't fetch system configuration mode from the L2 " + "at '"
                                 + theURL + "'. Is the L2 running?");
  }

  public int getSessionLockType(String appName) {
    return configHelper.getSessionLockType(appName);
  }
}

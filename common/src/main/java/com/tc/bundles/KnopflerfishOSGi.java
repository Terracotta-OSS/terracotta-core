/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.knopflerfish.framework.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.terracottatech.config.Module;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Embedded KnopflerFish OSGi implementation, see the <a href="http://www.knopflerfish.org/">Knopflerfish
 * documentation</a> for more details.
 */
final class KnopflerfishOSGi extends AbstractEmbeddedOSGiRuntime {

  private static final TCLogger logger                        = TCLogging.getLogger(KnopflerfishOSGi.class);
  private static final TCLogger consoleLogger                 = CustomerLogging.getConsoleLogger();

  private static final String   KF_BUNDLESTORAGE_PROP         = "org.knopflerfish.framework.bundlestorage";
  private static final String   KF_BUNDLESTORAGE_PROP_DEFAULT = "memory";

  private final URL[]           repositories;
  private final Framework       framework;
  private final Resolver        resolver;

  static {
    System.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, "*");
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
  }

  KnopflerfishOSGi(final URL[] bundleRepositories, Collection<Repository> addlRepos) throws Exception {
    this.repositories = bundleRepositories;

    final ProductInfo info = ProductInfo.getInstance();
    resolver = new Resolver(ResolverUtils.urlsToStrings(repositories), true, info.mavenArtifactsVersion(),
                            info.timApiVersion(), addlRepos);

    System.setProperty("org.knopflerfish.osgi.registerserviceurlhandler", "false");
    framework = new Framework(null);
    framework.launch(0);

  }

  public URL[] getRepositories() {
    return this.repositories;
  }

  public Map<Bundle, URL> installBundles(final URL[] locations) throws BundleException {
    Map<Bundle, URL> rv = new HashMap<Bundle, URL>();

    for (URL location : locations) {
      Bundle bundle = installBundle(location);
      rv.put(bundle, location);
    }
    return rv;
  }

  public void startBundles(final URL[] locations, final EmbeddedOSGiEventHandler handler) throws BundleException {
    for (URL location : locations) {
      final long id = framework.getBundleId(location.toString());
      startBundle(id, handler);
    }
  }

  private void startBundle(final long id, final EmbeddedOSGiEventHandler handler) throws BundleException {
    Assert.assertNotNull(handler);
    final Bundle bundle = framework.bundles.getBundle(id);
    final boolean isStarting = ((bundle.getState() & Bundle.STARTING) == Bundle.STARTING);
    final boolean isActive = ((bundle.getState() & Bundle.ACTIVE) == Bundle.ACTIVE);

    if (isActive || isStarting) {
      warn(Message.WARN_SKIPPED_ALREADY_ACTIVE, new Object[] { bundle.getSymbolicName() });
      return;
    }

    if (logger.isDebugEnabled()) {
      info(Message.STARTING_BUNDLE, new Object[] { bundle.getSymbolicName() });
    }

    try {
      framework.startBundle(bundle.getBundleId());
    } catch (BundleException be) {
      consoleLogger.error("Failed to start bundle: " + bundle.getSymbolicName(), be);
      throw be;
    }

    if ((bundle.getState() & Bundle.ACTIVE) != bundle.getState()) {
      StringBuffer msg = new StringBuffer();
      msg.append("Failed to start bundle: ").append(bundle.getSymbolicName());
      msg.append(" - bundle state remains inactive.");
      msg.append(" (").append(bundle.getLocation()).append(")");
      throw new BundleException(msg.toString());
    }
    handler.callback(bundle);
  }

  final String versionCheckMode() throws BundleException {
    String mode = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, true);
    if ((mode == null) || (mode.length() == 0)) mode = IVersionCheck.OFF;
    ArrayList modes = new ArrayList();
    modes.add(IVersionCheck.OFF);
    modes.add(IVersionCheck.WARN);
    modes.add(IVersionCheck.ENFORCE);
    modes.add(IVersionCheck.STRICT);
    String msg = "Invalid tc-version-check mode, use one of the following values: " + modes.toString();
    if (!modes.contains(mode)) throw new BundleException(msg);
    return mode;
  }

  final int versionCheck(String mode, String requireversion, String tcversion) {
    if (mode.equals(IVersionCheck.OFF)) return IVersionCheck.IGNORED;

    String actual = tcversion.replace('-', '.');
    String expected = (requireversion == null) || (requireversion.length() == 0) ? "" : requireversion
        .replace('-', '.');

    if ((expected.length() > 0) && (!Pattern.matches(IConstants.OSGI_VERSION_PATTERN.pattern(), expected))) return IVersionCheck.ERROR_BAD_REQUIRE_ATTRIBUTE;

    if (mode.equals(IVersionCheck.WARN)) {
      if (expected.length() == 0) return IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING;
      if (!expected.equals(actual)) return IVersionCheck.WARN_INCORRECT_VERSION;
    }

    if (mode.equals(IVersionCheck.ENFORCE)) {
      if (expected.length() == 0) return IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING;
      if (!expected.equals(actual)) return IVersionCheck.ERROR_INCORRECT_VERSION;
    }

    if (mode.equals(IVersionCheck.STRICT)) {
      if (expected.length() == 0) return IVersionCheck.ERROR_REQUIRE_ATTRIBUTE_MISSING;
      if (!expected.equals(actual)) return IVersionCheck.ERROR_INCORRECT_VERSION;
    }

    return IVersionCheck.OK;
  }

  final void versionCheck(Bundle bundle) throws BundleException {
    final String mode = versionCheckMode();
    final String version = (String) bundle.getHeaders().get("Terracotta-RequireVersion");
    final String tcversion = ProductInfo.getInstance().version();

    final String BAD_REQUIRE_ATTRIBUTE = "The Terracotta-RequireVersion attribute defined in "
                                         + bundle.getSymbolicName()
                                         + " module is invalid, this value must match the regular expression pattern: "
                                         + IConstants.OSGI_VERSION_PATTERN.pattern();
    final String REQUIRE_ATTRIBUTE_MISSING = "The Terracotta-RequireVersion attribute is not defined in "
                                             + bundle.getSymbolicName() + " module.";
    final String INCORRECT_VERSION = "The Terraccotta client version is '" + tcversion + "' but the "
                                     + bundle.getSymbolicName() + " module requires '" + version + "'";
    switch (versionCheck(mode, version, tcversion)) {
      case IVersionCheck.OK:
        // ignored
        break;
      case IVersionCheck.IGNORED:
        // log???
        break;
      case IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING:
        consoleLogger.warn(REQUIRE_ATTRIBUTE_MISSING);
        logger.warn(REQUIRE_ATTRIBUTE_MISSING);
        break;
      case IVersionCheck.WARN_INCORRECT_VERSION:
        consoleLogger.warn(INCORRECT_VERSION);
        logger.warn(INCORRECT_VERSION);
        break;
      case IVersionCheck.ERROR_REQUIRE_ATTRIBUTE_MISSING:
        consoleLogger.fatal(REQUIRE_ATTRIBUTE_MISSING);
        throw new BundleException(REQUIRE_ATTRIBUTE_MISSING);
        // fatal(REQUIRE_ATTRIBUTE_MISSING);
      case IVersionCheck.ERROR_INCORRECT_VERSION:
        consoleLogger.fatal(INCORRECT_VERSION);
        throw new BundleException(INCORRECT_VERSION);
        // fatal(INCORRECT_VERSION);
      case IVersionCheck.ERROR_BAD_REQUIRE_ATTRIBUTE:
        consoleLogger.fatal(BAD_REQUIRE_ATTRIBUTE);
        throw new BundleException(BAD_REQUIRE_ATTRIBUTE);
        // fatal(BAD_REQUIRE_ATTRIBUTE);
    }
  }

  public Bundle installBundle(final URL location) throws BundleException {
    try {
      if (logger.isDebugEnabled()) info(Message.INSTALLING_BUNDLE, new Object[] { location });

      final long id = framework.installBundle(location.toString(), location.openStream());
      final Bundle bundle = framework.getSystemBundleContext().getBundle(id);
      final String symname = bundle.getSymbolicName();

      if (symname == null) {
        framework.uninstallBundle(id);
        warn(Message.WARN_SKIPPED_FILE_INSTALLATION, new Object[] { location });
      } else if (logger.isDebugEnabled()) {
        info(Message.BUNDLE_INSTALLED, new Object[] { symname });
      }

      versionCheck(bundle);

      return bundle;
    } catch (Exception e) {
      exception(Message.ERROR_BUNDLE_INACCESSIBLE, new Object[] { location.toString() }, e);

      // unreachable (although the compiler doesn't know it)
      throw new AssertionError();
    }
  }

  public void registerService(final Object serviceObject, final Dictionary serviceProps) {
    if (logger.isDebugEnabled()) {
      info(Message.REGISTERING_SERVICE, new Object[] { serviceObject.getClass().getName(), serviceProps });
    }
    framework.getSystemBundleContext().registerService(serviceObject.getClass().getName(), serviceObject, serviceProps);
    if (logger.isDebugEnabled()) {
      info(Message.SERVICE_REGISTERED, new Object[] { serviceObject.getClass().getName() });
    }
  }

  public void registerService(final String serviceName, final Object serviceObject, final Dictionary serviceProps) {
    if (logger.isDebugEnabled()) {
      info(Message.REGISTERING_SERVICE, new Object[] { serviceName, serviceProps });
    }
    framework.getSystemBundleContext().registerService(serviceName, serviceObject, serviceProps);
    if (logger.isDebugEnabled()) {
      info(Message.SERVICE_REGISTERED, new Object[] { serviceName });
    }
  }

  public ServiceReference[] getAllServiceReferences(String clazz, java.lang.String filter)
      throws InvalidSyntaxException {
    return framework.getSystemBundleContext().getAllServiceReferences(clazz, filter);
  }

  public Object getService(ServiceReference service) {
    return framework.getSystemBundleContext().getService(service);
  }

  public void ungetService(ServiceReference service) {
    framework.getSystemBundleContext().ungetService(service);
  }

  public void shutdown() {
    if (framework == null) return;
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_FRAMEWORK, new Object[0]);
    }
    framework.shutdown();
    info(Message.SHUTDOWN, new Object[0]);
  }

  public URL[] resolve(Module[] modules) throws BundleException {
    return resolver.resolve(modules);
  }

  public URL resolveToolkitIfNecessary() throws BundleException {
    return resolver.attemptToolkitFreeze();
  }
}

/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.knopflerfish.framework.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.util.URLUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Dictionary;

/**
 * Embedded KnopflerFish OSGi implementation, see the <a href="http://www.knopflerfish.org/">Knopflerfish documentation</a>
 * for more details.
 */
final class KnopflerfishOSGi extends AbstractEmbeddedOSGiRuntime {

  private static final TCLogger logger                        = TCLogging.getLogger(KnopflerfishOSGi.class);

  private static String         KF_BUNDLESTORAGE_PROP         = "org.knopflerfish.framework.bundlestorage";
  private static String         KF_BUNDLESTORAGE_PROP_DEFAULT = "memory";

  // {0} := bundle name, {1} := bundle version (not necessarily numeric)
  private static final String   BUNDLE_PATH                   = "{0}-{1}.jar";

  private final URL[]           bundleRepositories;
  private final Framework       framework;

  static {
    System.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, "*");
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
    /*
     try {
     framework = new Framework(null);
     framework.launch(0);
     }
     catch (Exception e) {
     throw new RuntimeException("Error initializing OSGi framework", e);
     }
     */
  }

  /**
   * Creates and starts an in-memory OSGi layer using Knopflerfish.
   */
  public KnopflerfishOSGi(final URL[] bundleRepositories) throws Exception {
    this.bundleRepositories = bundleRepositories;
    System.setProperty("org.knopflerfish.osgi.registerserviceurlhandler", "false");
    framework = new Framework(null);
    framework.launch(0);
  }

  public void installBundles() throws BundleException {
    for (int i = 0; i < bundleRepositories.length; i++) {
      final URL bundleLocation = bundleRepositories[i];
      if (bundleLocation.getProtocol().equalsIgnoreCase("file")) {
        final File bundleDir = new File(bundleLocation.getFile());
        if (!bundleDir.exists()) {
          // TODO: We need a better way to handle this. If we throw an exception 
          // here, and we're running a test, then it breaks the test;
          // but if we're in production and 'bundleDir' does not exists, then
          // the client wont be notified of the anomaly; maybe print a warning message???
          logger.warn("The bundle repository: '" + bundleDir.toString() + "' does not exist.");
          continue;
        }

        if (!bundleDir.isDirectory()) { throw new BundleException("Invalid bundle repository specified in the URL ["
            + bundleDir + "]"); }

        final String MODULE_VERSION_REGEX = "[0-9]+\\.[0-9]+\\.[0-9]+";
        final String MODULE_FILENAME_REGEX = ".+-.+-";
        final String MODULE_FILENAME_EXT_REGEX = "\\.jar";
        final File[] bundleFiles = bundleDir.listFiles(new FileFilter() {
          public boolean accept(File file) {
            return file.isFile()
                && file.getName().matches(MODULE_FILENAME_REGEX + MODULE_VERSION_REGEX + MODULE_FILENAME_EXT_REGEX);
          }
        });

        for (int j = 0; j < bundleFiles.length; j++) {
          final String bundleName = bundleFiles[j].getName().replaceFirst(
              "-" + MODULE_VERSION_REGEX + MODULE_FILENAME_EXT_REGEX, "");
          final String bundleVersion = bundleFiles[j].getName().replaceFirst(MODULE_FILENAME_REGEX, "").replaceFirst(
              MODULE_FILENAME_EXT_REGEX, "");
          installBundle(bundleName, bundleVersion);
        }
      }
    }
  }

  public void installBundle(final String bundleName, final String bundleVersion) throws BundleException {
    final URL bundleLocation = getBundleURL(bundleName, bundleVersion);
    if (bundleLocation != null) {
      try {
        framework.installBundle(bundleLocation.toString(), bundleLocation.openStream());
        info(Message.BUNDLE_INSTALLED, new Object[] { getSymbolicName(bundleName, bundleVersion) });
      } catch (IOException ioe) {
        throw new BundleException("Unable to open URL [" + bundleLocation + "]", ioe);
      }
    } else {
      throw new BundleException("Unable to find bundle '" + bundleName + "', version '" + bundleVersion
          + "' from the default repository or any of the repositories listed in your config");
    }
  }

  private Bundle findBundleBySymbolicName(final RequiredBundleSpec spec) {
    final Bundle[] bundles = framework.getSystemBundleContext().getBundles();
    for (int i = 0; i < bundles.length; i++) {
      Bundle bundle = bundles[i];
      final String symbolicName = (String) bundle.getHeaders().get("Bundle-SymbolicName");
      final String version = (String) bundle.getHeaders().get("Bundle-Version");
      if (spec.isCompatible(symbolicName, version)) { return bundle; }
    }
    return null;
  }

  private void startBundle(final long bundleId, final EmbeddedOSGiRuntimeCallbackHandler handler)
      throws BundleException {
    final Bundle bundle = framework.bundles.getBundle(bundleId);
    final String requires = (String) bundle.getHeaders().get("Require-Bundle");

    final String[] bundles = RequiredBundleSpec.parseList(requires);
    for (int i = 0; i < bundles.length; i++) {
      final RequiredBundleSpec spec = new RequiredBundleSpec(bundles[i]);
      final Bundle reqdBundle = findBundleBySymbolicName(spec);
      if (reqdBundle == null) { throw new BundleException("No compatible bundle installed for the required bundle: "
          + spec.getSymbolicName() + ", bundle-version: " + spec.getBundleVersion()); }
      startBundle(reqdBundle.getBundleId(), handler);
    }

    framework.startBundle(bundle.getBundleId());
    info(Message.BUNDLE_STARTED, new Object[] { bundle.getSymbolicName() });
    handler.callback(bundle);
  }

  public void startBundle(final String bundleName, final String bundleVersion,
                          final EmbeddedOSGiRuntimeCallbackHandler handler) throws BundleException {
    startBundle(getBundleID(bundleName, bundleVersion), handler);
  }

  public Bundle getBundle(String bundleName, String bundleVersion) throws BundleException {
    return framework.bundles.getBundle(getBundleID(bundleName, bundleVersion));
  }

  public void registerService(final Object serviceObject, final Dictionary serviceProps) throws BundleException {
    framework.getSystemBundleContext().registerService(serviceObject.getClass().getName(), serviceObject, serviceProps);
    info(Message.SERVICE_REGISTERED, new Object[] { serviceObject.getClass().getName() });
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

  public void stopBundle(final String bundleName, final String bundleVersion) throws BundleException {
    final long bundleID = getBundleID(bundleName, bundleVersion);
    framework.stopBundle(bundleID);
    info(Message.BUNDLE_STOPPED, new Object[] { getSymbolicName(bundleName, bundleVersion) });
  }

  public void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException {
    framework.uninstallBundle(getBundleID(bundleName, bundleVersion));
    info(Message.BUNDLE_UNINSTALLED, new Object[] { getSymbolicName(bundleName, bundleVersion) });
  }

  public void shutdown() throws BundleException {
    framework.shutdown();
    info(Message.SHUTDOWN, new Object[0]);
  }

  private URL getBundleURL(final String bundleName, final String bundleVersion) throws BundleException {
    final String path = MessageFormat.format(BUNDLE_PATH, new String[] { bundleName, bundleVersion });
    try {
      final URL url = URLUtil.resolve(bundleRepositories, path);
      if (url == null) { throw new BundleException("Unable to locate the bundle '" + bundleName + "' for version '"
          + bundleVersion + "', please check that module name and version number you specified is correct."); }
      return url;
    } catch (MalformedURLException murle) {
      throw new BundleException("Unable to resolve bundle '" + path
          + "', please check that your repositories are correctly configured", murle);
    }
  }

  private long getBundleID(final String bundleName, final String bundleVersion) throws BundleException {
    final URL bundleURL = getBundleURL(bundleName, bundleVersion);
    return framework.getBundleId(bundleURL.toString());
  }

  private String getSymbolicName(final String bundleName, final String bundleVersion) throws BundleException {
    final Bundle bundle = framework.getSystemBundleContext().getBundle(getBundleID(bundleName, bundleVersion));
    return bundle.getSymbolicName();
  }

}

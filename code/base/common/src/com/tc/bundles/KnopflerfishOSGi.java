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

  private static final String   BUNDLE_FILENAME_EXT           = ".jar";
  // {0} := bundle name, {1} := bundle version
  private static final String   BUNDLE_PATH                   = "{0}-{1}" + BUNDLE_FILENAME_EXT;
  private static final String   MODULE_VERSION_REGEX          = "[0-9]+\\.[0-9]+\\.[0-9]+";
  private static final String   MODULE_FILENAME_REGEX         = ".+-";
  private static final String   MODULE_FILENAME_EXT_REGEX     = "\\" + BUNDLE_FILENAME_EXT;
  private static final String   BUNDLE_FILENAME_PATTERN       = "^" + MODULE_FILENAME_REGEX + MODULE_VERSION_REGEX
                                                                  + MODULE_FILENAME_EXT_REGEX + "$";

  private final URL[]           bundleRepositories;
  private final String          defaultGroupId;  
  private final Framework       framework;

  static {
    System.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, "*");
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
  }

  /**
   * Creates and starts an in-memory OSGi layer using Knopflerfish.
   */
  public KnopflerfishOSGi(final String groupId, final URL[] bundleRepositories) throws Exception {
    this.bundleRepositories = bundleRepositories;
    this.defaultGroupId     = groupId;
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
          logger.warn("The bundle repository: '" + bundleDir + "' does not exist.");
          continue;
        }

        if (!bundleDir.isDirectory()) {
          throw new BundleException("Invalid bundle repository specified in the URL [" + bundleDir + "]");
        }

        
        final File[] bundleFiles = findBundleFiles(bundleDir);
        for (int j = 0; j < bundleFiles.length; j++) {
          installBundle(getBundleName(bundleFiles[j]), getBundleVersion(bundleFiles[j]));
        }
      }
    }
  }

  public void installBundle(final String bundleName, final String bundleVersion) throws BundleException {
    final URL bundleLocation = getBundleURL(bundleName, bundleVersion);
    if (bundleLocation != null) {
      try {
        if (logger.isDebugEnabled()) {
          info(Message.INSTALLING_BUNDLE, new Object[] { bundleLocation });
        }
        final long bundleId = framework.installBundle(bundleLocation.toString(), bundleLocation.openStream());
        final String symbolicName = getSymbolicName(bundleName, bundleVersion);
        if (symbolicName == null) {
          framework.uninstallBundle(bundleId);
          final String msg = MessageFormat.format("Skipped config-bundle installation of file: " + BUNDLE_PATH
              + ", it does not appear to be a valid config-bundle file.", new String[] { bundleName, bundleVersion });
          logger.warn(msg);
        } else {
          info(Message.BUNDLE_INSTALLED, new Object[] { symbolicName });
        }
      } catch (IOException ioe) {
        throw new BundleException("Unable to open URL [" + bundleLocation + "]", ioe);
      }
    } else {
      throw new BundleException("Unable to find bundle '" + bundleName + "', version '" + bundleVersion
          + "' from the default repository or any of the repositories listed in your config");
    }
  }

  /**
   * Finds all files in the given <code>bundleDir</code> for which {@link #isBundleFileName(File)}
   * is <code>true</code> and returns them as a File[].
   */
  protected File[] findBundleFiles(final File bundleDir) {
    return bundleDir.listFiles(new FileFilter() {
      public boolean accept(final File file) {
        final boolean isOk = isBundleFileName(file);
        if (!isOk) {
          final String msg = MessageFormat.format("Skipped config-bundle installation of file: {0}, "
              + "config-bundle filenames are expected to conform to the following pattern: {1}", new String[] {
              file.getName(), BUNDLE_FILENAME_PATTERN });
          logger.warn(msg);
        }
        return isOk;
      }
    });
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

    if (logger.isDebugEnabled()) {
      info(Message.STARTING_BUNDLE, new Object[] { bundle.getSymbolicName() });
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
    if (logger.isDebugEnabled()) {
      info(Message.REGISTERING_SERVICE, new Object[] { serviceObject.getClass().getName(), serviceProps });
    }
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
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_BUNDLE, new Object[] { getSymbolicName(bundleName, bundleVersion) });
    }
    framework.stopBundle(bundleID);
    info(Message.BUNDLE_STOPPED, new Object[] { getSymbolicName(bundleName, bundleVersion) });
  }

  public void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException {
    if (logger.isDebugEnabled()) {
      info(Message.UNINSTALLING_BUNDLE, new Object[] { getSymbolicName(bundleName, bundleVersion) });
    }
    framework.uninstallBundle(getBundleID(bundleName, bundleVersion));
    info(Message.BUNDLE_UNINSTALLED, new Object[] { getSymbolicName(bundleName, bundleVersion) });
  }

  public void shutdown() throws BundleException {
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_FRAMEWORK, new Object[0]);
    }
    framework.shutdown();
    info(Message.SHUTDOWN, new Object[0]);
  }

  private URL getBundleURL(final String bundleName, final String bundleVersion) throws BundleException {
    final String path = MessageFormat.format(BUNDLE_PATH, new String[] { bundleName, bundleVersion });
    try {
      final URL url = URLUtil.resolve(bundleRepositories, path);
      if (url == null) {
        final String msg = MessageFormat
            .format(
                "Unable to locate the config-bundle file for bundle ''{0}'' version ''{1}'', "
                    + "please check that module name and version number you specified is correct; "
                    + "config-bundle filenames are extrapolated by concatenating the bundle name and version number, the resulting "
                    + "filename is expected to conform to the following pattern: {2}", new String[] { bundleName,
                    bundleVersion, BUNDLE_FILENAME_PATTERN });
        throw new BundleException(msg);
      }
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

  private String getBundleVersion(File bundleFile) {
    return bundleFile.getName().replaceFirst(MODULE_FILENAME_REGEX, "").replaceFirst(
        MODULE_FILENAME_EXT_REGEX, "");
  }

  private String getBundleName(File bundleFile) {
    return bundleFile.getName().replaceFirst(
        "-" + MODULE_VERSION_REGEX + MODULE_FILENAME_EXT_REGEX, "");
  }

  /**
   * Returns <code>true</code> if the given File object refers to a file matching the
   * {@link KnopflerfishOSGi#BUNDLE_FILENAME_PATTERN}, <code>false</code> otherwise.
   */
  protected boolean isBundleFileName(final File file) {
    return file.isFile() && file.getName().matches(BUNDLE_FILENAME_PATTERN);
  }

}

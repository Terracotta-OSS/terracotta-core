/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

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
  private static final String   BUNDLE_VERSION_REGEX          = "[0-9]+\\.[0-9]+\\.[0-9]+";
  private static final String   BUNDLE_FILENAME_REGEX         = ".+-";
  private static final String   BUNDLE_FILENAME_EXT_REGEX     = "\\" + BUNDLE_FILENAME_EXT;
  private static final String   BUNDLE_FILENAME_PATTERN       = "^" + BUNDLE_FILENAME_REGEX + BUNDLE_VERSION_REGEX
                                                                  + BUNDLE_FILENAME_EXT_REGEX + "$";

  private final URL[]           bundleRepositories;
  private final Framework       framework;

  static {
    System.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, "*");
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
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
      // TODO: Add support for protocols other than file://
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

        if (bundleDir.isDirectory()) {
          final File[] bundleFiles = findBundleFiles(bundleDir);
          for (int j = 0; j < bundleFiles.length; j++) {
            installBundle(bundleFiles[j]);
          }
        } else {
          throw new BundleException("Invalid bundle repository specified in the URL [" + bundleDir + "]");
        }
      }
    }
  }

  private void installBundle(final File bundle) throws BundleException {
    URL bundleLocation = null;
    try {
      bundleLocation = bundle.toURL();
      installBundle(bundleLocation);
    } catch (MalformedURLException mue) {
      throw new BundleException("Invalid bundle URL [" + bundleLocation.toString() + "]", mue);
    }
  }

  private void installBundle(final URL bundle) throws BundleException {
    try {
      if (logger.isDebugEnabled()) {
        info(Message.INSTALLING_BUNDLE, new Object[] { bundle });
      }
      final long bundleId = framework.installBundle(bundle.toString(), bundle.openStream());
      final String symbolicName = getSymbolicName(bundleId);
      if (symbolicName == null) {
        framework.uninstallBundle(bundleId);
        // TODO: Fix this, we shouldn't have to do this much just to log a warning...
        final File bundleFile = new File(new URI(bundle.toString()));
        final String name = getBundleName(bundleFile);
        final String version = getBundleVersion(bundleFile);
        final String msg = MessageFormat.format("Skipped config-bundle installation of file: " + BUNDLE_PATH
            + ", it does not appear to be a valid config-bundle file.", new String[] { name, version });
        logger.warn(msg);
      } else {
        info(Message.BUNDLE_INSTALLED, new Object[] { symbolicName });
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    } catch (IOException ioe) {
      throw new BundleException("Unable to open URL [" + bundle.toString() + "]", ioe);
    }
  }

  public void installBundle(final String name, final String version, final String groupId) throws BundleException {
    final URL bundleLocation = getBundleURL(name, version, groupId);
    if (bundleLocation == null) throw new BundleException("Unable to find bundle '" + name + "', version '" + version
        + "' for group-id '" + groupId
        + "' from the default repository or any of the repositories listed in your config");
    installBundle(bundleLocation);
  }

  /**
   * Find all of the config-bundle files stored in <code>repository</code> It assumes that repository is a directory
   * organized like Maven's repository directory structure.
   * 
   * @param repository
   * @return An array of File listing all of the config-bundle files found.
   */
  protected File[] findBundleFiles(final File repository) {
    List list = new ArrayList();
    Collection jarFiles = FileUtils.listFiles(repository, new String[] { "jar" }, true);
    for (Iterator i = jarFiles.iterator(); i.hasNext();) {
      File jarFile = (File) i.next();
      if (isBundleFileName(jarFile)) {
        list.add(jarFile);
      } else {
        final String msg = MessageFormat.format(
            "Skipped config-bundle installation of file: {0}, config-bundle filenames are expected "
                + "to conform to the following pattern: {1}",
            new String[] { jarFile.getName(), BUNDLE_FILENAME_PATTERN });
        logger.warn(msg);
      }
    }
    return (File[]) list.toArray(new File[list.size()]);
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

  public void startBundle(final String name, final String version, final String groupId,
                          final EmbeddedOSGiRuntimeCallbackHandler handler) throws BundleException {
    startBundle(getBundleID(name, version, groupId), handler);
  }

  public Bundle getBundle(final String name, final String version, final String groupId) throws BundleException {
    return framework.bundles.getBundle(getBundleID(name, version, groupId));
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

  public void stopBundle(final String name, final String version, final String groupId) throws BundleException {
    final long bundleID = getBundleID(name, version, groupId);
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_BUNDLE, new Object[] { getSymbolicName(name, version, groupId) });
    }
    framework.stopBundle(bundleID);
    info(Message.BUNDLE_STOPPED, new Object[] { getSymbolicName(name, version, groupId) });
  }

  public void uninstallBundle(final String name, final String version, final String groupId) throws BundleException {
    if (logger.isDebugEnabled()) {
      info(Message.UNINSTALLING_BUNDLE, new Object[] { getSymbolicName(name, version, groupId) });
    }
    framework.uninstallBundle(getBundleID(name, version, groupId));
    info(Message.BUNDLE_UNINSTALLED, new Object[] { getSymbolicName(name, version, groupId) });
  }

  public void shutdown() throws BundleException {
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_FRAMEWORK, new Object[0]);
    }
    framework.shutdown();
    info(Message.SHUTDOWN, new Object[0]);
  }

  private URL getBundleURL(final String name, final String version, final String groupId) throws BundleException {
    final String base = groupId.replace('.', File.separatorChar);
    final String path = MessageFormat.format("{2}{3}{0}{3}{1}{3}" + BUNDLE_PATH, new String[] { name, version,
        base, File.separator });
    try {
      final URL url = URLUtil.resolve(bundleRepositories, path);
      if (url == null) {
        final String msg = MessageFormat
            .format(
                "Unable to locate the config-bundle file for bundle ''{0}'' version ''{1}'' using group-id ''{2}'', "
                    + "please check that module name and version number you specified is correct; "
                    + "config-bundle filenames are extrapolated by concatenating the bundle name and version number, the resulting "
                    + "filename is expected to conform to the following pattern: {3}", new String[] { name, version,
                    groupId, BUNDLE_FILENAME_PATTERN });
        throw new BundleException(msg);
      }
      return url;
    } catch (MalformedURLException murle) {
      throw new BundleException("Unable to resolve bundle '" + path
          + "', please check that your repositories are correctly configured", murle);
    }
  }

  private long getBundleID(final String name, final String version, final String groupId) throws BundleException {
    final URL bundleURL = getBundleURL(name, version, groupId);
    return framework.getBundleId(bundleURL.toString());
  }

  private String getSymbolicName(final String name, final String version, final String groupId) throws BundleException {
    final Bundle bundle = framework.getSystemBundleContext().getBundle(getBundleID(name, version, groupId));
    return bundle.getSymbolicName();
  }

  private String getSymbolicName(final long id) throws BundleException {
    final Bundle bundle = framework.getSystemBundleContext().getBundle(id);
    return bundle.getSymbolicName();
  }

  // ---

  private String getBundleVersion(File bundleFile) {
    return bundleFile.getName().replaceFirst(BUNDLE_FILENAME_REGEX, "").replaceFirst(BUNDLE_FILENAME_EXT_REGEX, "");
  }

  private String getBundleName(File bundleFile) {
    return bundleFile.getName().replaceFirst("-" + BUNDLE_VERSION_REGEX + BUNDLE_FILENAME_EXT_REGEX, "");
  }

  /**
   * Returns <code>true</code> if the given File object refers to a file matching the
   * {@link KnopflerfishOSGi#BUNDLE_FILENAME_PATTERN}, <code>false</code> otherwise.
   */
  protected boolean isBundleFileName(final File file) {
    return file.isFile() && file.getName().matches(BUNDLE_FILENAME_PATTERN);
  }

}

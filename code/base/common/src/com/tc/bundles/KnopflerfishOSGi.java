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
import com.terracottatech.config.Module;

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

  public void installBundles(Module[] bundles) throws BundleException {
    for (int i = 0; i < bundles.length; i++) {
      Module bundle = bundles[i];
      URL bundleURL = getBundleURL(bundle);
      installBundle(bundleURL);
    }
    
//    for (int i = 0; i < bundleRepositories.length; i++) {
//      final URL location = bundleRepositories[i];
//      // TODO: Add support for protocols other than file://
//      if (location.getProtocol().equalsIgnoreCase("file")) {
//        final File repository = new File(location.getFile());
//        if (!repository.exists()) {
//          // TODO: We need a better way to handle this. If we throw an exception
//          // here, and we're running a test, then it breaks the test;
//          // but if we're in production and 'bundleDir' does not exists, then
//          // the client wont be notified of the anomaly; maybe print a warning message???
//          warn(Message.WARN_MISSING_REPOSITORY, new Object[] { repository });
//          continue;
//        }
//
//        if (repository.isDirectory()) {
//          final File[] bundleFiles = findBundleFiles(repository);
//          for (int j = 0; j < bundleFiles.length; j++) {
//            installBundle(bundleFiles[j]);
//          }
//          return;
//        }
//
//        exception(Message.ERROR_INVALID_REPOSITORY, new Object[] { repository }, null);
//      }
//    }
  }

  public void startBundles(final Module[] bundles, final EmbeddedOSGiRuntimeCallbackHandler handler)
      throws BundleException {
    for (int i = 0; i < bundles.length; ++i) {
      startBundle(bundles[i], handler);
    }
  }

  private void installBundle(final File bundle) throws BundleException {
    try {
      URL bundleURL = bundle.toURL();
      installBundle(bundleURL);
    } catch (MalformedURLException mue) {
      exception(Message.ERROR_INVALID_REPOSITORY, new Object[] { bundle }, mue);
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
        final File bundleFile = new File(new URI(bundle.toString()));
        warn(Message.WARN_SKIPPED_FILE_INSTALLATION, new Object[] { bundleFile.getName() });
      } else {
        info(Message.BUNDLE_INSTALLED, new Object[] { symbolicName });
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      exception(Message.ERROR_BUNDLE_INACCESSIBLE, new Object[] { bundle.toString() }, e);
    }
  }

  public void installBundle(final Module bundle) throws BundleException {
    final URL location = getBundleURL(bundle);
    installBundle(location);
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
      if (isValidFilename(jarFile)) {
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

  /**
   * Locate and return the symbolic name of an installed bundle given a <code>RequiredBundleSpec</code>.
   * 
   * @return The symbolic name of the bundle, meaning: it was installed, and it was a config-bundle, and it conforms to
   *         the <code>spec</code> - otherwise return <code>null</code>
   */
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

  public void startBundle(final Module bundle, final EmbeddedOSGiRuntimeCallbackHandler handler) throws BundleException {
    final long id = getBundleId(bundle);
    startBundle(id, handler);
  }

  public Bundle getBundle(final Module bundle) throws BundleException {
    final long id = getBundleId(bundle);
    return framework.bundles.getBundle(id);
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

  public void stopBundle(final Module bundle) throws BundleException {
    final long id = getBundleId(bundle);
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_BUNDLE, new Object[] { getSymbolicName(id) });
    }
    framework.stopBundle(id);
    info(Message.BUNDLE_STOPPED, new Object[] { getSymbolicName(id) });
  }

  public void uninstallBundle(final Module bundle) throws BundleException {
    final long id = getBundleId(bundle);
    if (logger.isDebugEnabled()) {
      info(Message.UNINSTALLING_BUNDLE, new Object[] { getSymbolicName(id) });
    }
    framework.uninstallBundle(id);
    info(Message.BUNDLE_UNINSTALLED, new Object[] { getSymbolicName(id) });
  }

  public void shutdown() throws BundleException {
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_FRAMEWORK, new Object[0]);
    }
    framework.shutdown();
    info(Message.SHUTDOWN, new Object[0]);
  }

  private URL getBundleURL(final Module bundle) throws BundleException {
    final String name = bundle.getName();
    final String version = bundle.getVersion();
    final String groupId = bundle.getGroupId();
    final String base = groupId.replace('.', File.separatorChar);
    final String path = MessageFormat.format("{2}{3}{0}{3}{1}{3}" + BUNDLE_PATH, new String[] { name, version, base,
        File.separator });
    URL url = null;
    try {
      url = URLUtil.resolve(bundleRepositories, path);
      if (url == null) {
        exception(Message.ERROR_BUNDLE_NOT_FOUND, new Object[] { name, version, groupId, BUNDLE_FILENAME_PATTERN },
            null);
      }
    } catch (MalformedURLException e) {
      exception(Message.ERROR_BUNDLE_URL_UNRESOLVABLE, new Object[] { path }, e);
    }
    return url;
  }

  private long getBundleId(final Module bundle) throws BundleException {
    final URL location = getBundleURL(bundle);
    return framework.getBundleId(location.toString());
  }

  private String getSymbolicName(final long id) throws BundleException {
    final Bundle bundle = framework.getSystemBundleContext().getBundle(id);
    return bundle.getSymbolicName();
  }

  /**
   * Returns <code>true</code> if the given File object refers to a file matching the
   * {@link KnopflerfishOSGi#BUNDLE_FILENAME_PATTERN}, <code>false</code> otherwise.
   * 
   * @return <code>true</code> if the name of the file conforms to the <code>BUNDLE_FILENAME_PATTERN</code>
   */
  protected boolean isValidFilename(final File file) {
    return file.isFile() && file.getName().matches(BUNDLE_FILENAME_PATTERN);
  }

}

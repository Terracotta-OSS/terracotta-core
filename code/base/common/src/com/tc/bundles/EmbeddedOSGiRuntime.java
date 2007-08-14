/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.config.Directories;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracottatech.config.Modules;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * For OSGi information please refer to the documentation at the <a href="http://www.osgi.org/">OSGi web page</a>
 */
public interface EmbeddedOSGiRuntime {

  public static final String MODULES_URL_PROPERTY_NAME = "tc.tests.configuration.modules.url";

  void installBundles() throws BundleException;

  void installBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void startBundle(final String bundleName, final String bundleVersion, final EmbeddedOSGiRuntimeCallbackHandler handler)
      throws BundleException;

  Bundle getBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void registerService(final Object serviceObject, final Dictionary serviceProps) throws BundleException;

  void stopBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException;

  ServiceReference[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException;

  Object getService(final ServiceReference service);

  void ungetService(final ServiceReference service);

  /**
   * This should shut down the OSGi framework itself and all running bundles.
   */
  void shutdown() throws BundleException;

  static class Factory {

    private static final TCLogger logger = TCLogging.getLogger(EmbeddedOSGiRuntime.class);

    static String groupIdToRelativePath(final String groupId) {
      return groupId.replace('.', '/');
    }

    static String[] decomposeGroupId(final String groupId) {
      return groupId.split("\\.");
    }

    static File getPathFromGroupId(final File prefix, final String groupId) {
      final String[] groupPaths = decomposeGroupId(groupId);
      File path = prefix;
      for (int i = 0; i < groupPaths.length; i++) {
        path = new File(path, groupPaths[i]);
      }
      return path.getAbsoluteFile();
    }

    public static EmbeddedOSGiRuntime createOSGiRuntime(final Modules modules) throws BundleException, Exception {
      final List prependLocations = new ArrayList();

      // There are two repositories that we [optionally] prepend: a system property (used by tests)
      // and the installation root (which is not set when running tests)
      try {
        if (Directories.getInstallationRoot() != null) {
          final URL defaultRepository = getPathFromGroupId(new File(Directories.getInstallationRoot(), "modules"),
              modules.getGroupId()).toURL();
          // final URL defaultRepository = new File(Directories.getInstallationRoot(), "modules").toURL();
          logger.debug("Prepending default bundle repository: " + defaultRepository.toString());
          prependLocations.add(defaultRepository);
        }
      } catch (FileNotFoundException fnfe) {
        // Ignore, tc.install-dir is not set so we must be in a test environment
      }

      try {
        if (System.getProperty(MODULES_URL_PROPERTY_NAME) != null) {
          prependLocations.add(new URL(System.getProperty(MODULES_URL_PROPERTY_NAME)));
        }
        final URL[] prependURLs = new URL[prependLocations.size()];
        prependLocations.toArray(prependURLs);

        final URL[] bundleRepositories = new URL[modules.sizeOfRepositoryArray() + prependURLs.length];
        for (int pos = 0; pos < prependURLs.length; pos++) {
          bundleRepositories[pos] = prependURLs[pos];
        }

        if (prependURLs.length > 0) logger.info("OSGi Bundle Repositories:");
        for (int pos = prependURLs.length; pos < bundleRepositories.length; pos++) {
          // final String groupId = modules.getRepositoryArray(pos - prependURLs.length).getGroupId();
          // final String spec = groupIdToRelativePath(groupId == null ? modules.getGroupId() : groupId);
          // final URL context = new URL(modules.getRepositoryArray(pos - prependURLs.length).getStringValue());
          // bundleRepositories[pos] = new URL(context, spec);
          bundleRepositories[pos] = new URL(modules.getRepositoryArray(pos - prependURLs.length).getStringValue());
          logger.info("\t" + bundleRepositories[pos]);
        }

        return new KnopflerfishOSGi(modules.getGroupId(), bundleRepositories);
      } catch (MalformedURLException muex) {
        throw new BundleException(muex.getMessage());
      }
    }
  }

}

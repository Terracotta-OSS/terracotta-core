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

  void startBundle(final String bundleName, final String bundleVersion, final EmbeddedOSGiRuntimeCallbackHandler handler) throws BundleException;

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

    private static final TCLogger logger = TCLogging.getLogger(EmbeddedOSGiRuntime.Factory.class);

    public static EmbeddedOSGiRuntime createOSGiRuntime(final Modules modules) throws BundleException, Exception {
      final List prependLocations = new ArrayList();
      // There are two repositories that we [optionally] prepend: a system property (used by tests) and the installation
      // root (which is not set when running tests)
      try {
        if (Directories.getInstallationRoot() != null) {
          logger.debug("Prepending bundle repository: " + new File(Directories.getInstallationRoot(), "modules").toURL().toString());
          prependLocations.add(new File(Directories.getInstallationRoot(), "modules").toURL());
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
        
        for (int pos = prependURLs.length; pos < bundleRepositories.length; pos++) {
          bundleRepositories[pos] = new URL(modules.getRepositoryArray(pos - prependURLs.length));
        }
  
        logger.info("OSGi Bundle Repositories:");
        for (int i = 0; i < bundleRepositories.length; i++) {
          logger.info("\t" + bundleRepositories[i]);
        }
        
        return new KnopflerfishOSGi(bundleRepositories);
      } catch (MalformedURLException muex) {
        throw new BundleException(muex.getMessage());
      }
    }
  }

}

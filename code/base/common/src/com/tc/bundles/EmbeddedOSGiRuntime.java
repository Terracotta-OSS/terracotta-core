/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

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

  public static final String TESTS_CONFIG_MODULE_REPOSITORIES = "tc.tests.configuration.modules.url";
  public static final String TESTS_CONFIG_MODULE_NAMES        = "tc.tests.configuration.modules";

  URL[] getRepositories();

  void installBundles(final URL[] bundles) throws BundleException;

  void startBundles(final URL[] bundles, final EmbeddedOSGiEventHandler handler) throws BundleException;

  void registerService(final Object serviceObject, final Dictionary serviceProps) throws BundleException;

  ServiceReference[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException;

  Object getService(final ServiceReference service);

  void ungetService(final ServiceReference service);

  void shutdown();

  static class Factory {

    private static final TCLogger logger = TCLogging.getLogger(EmbeddedOSGiRuntime.class);

    private static final void injectDefaultRepository(final List prependLocations) throws FileNotFoundException, MalformedURLException {
      if (System.getProperty("tc.install-root") == null) return;

      final String defaultRepository = new File(Directories.getInstallationRoot(), "modules").toURL().toString();
      if (prependLocations.contains(defaultRepository)) return;

      logger.debug("Prepending default bundle repository: '" + defaultRepository + "'");
      prependLocations.add(defaultRepository);
    }

    private static final void injectTestRepository(final List prependLocations) throws FileNotFoundException, MalformedURLException  {
      String testRepository = System.getProperty(TESTS_CONFIG_MODULE_REPOSITORIES);
      if ((testRepository != null) && !prependLocations.contains(testRepository)) {
        logger.debug("Prepending test bundle repository: '" + testRepository.toString() + "'");
        prependLocations.add(testRepository);
      }

      // HACK - until we can propagate TESTS_CONFIG_MODULE_REPOSITORIES across VMs during test
      if (System.getProperty("tc.install-root") == null) return;
      
      final File installRoot = Directories.getInstallationRoot();
      if (installRoot.toString().endsWith("build")) return;

      final File buildRoot = new File(Directories.getInstallationRoot(), "build");
      testRepository = new File(buildRoot, "modules").toURL().toString();
      
      if (prependLocations.contains(testRepository)) return;
      prependLocations.add(testRepository);
    }

    public static EmbeddedOSGiRuntime createOSGiRuntime(final Modules modules) throws BundleException, Exception {
      final List prependLocations = new ArrayList();
      try {
        // XXX We shouldn't even worry about injecting 'test' repositories... 
        injectTestRepository(prependLocations);
        injectDefaultRepository(prependLocations);

        final String[] prependURLs = new String[prependLocations.size()];
        prependLocations.toArray(prependURLs);
        final URL[] bundleRepositories = new URL[modules.sizeOfRepositoryArray() + prependURLs.length];
        for (int pos = 0; pos < prependURLs.length; pos++) {
          bundleRepositories[pos] = new URL(prependURLs[pos]);
        }

        for (int pos = prependURLs.length; pos < bundleRepositories.length; pos++) {
          bundleRepositories[pos] = new URL(modules.getRepositoryArray(pos - prependURLs.length));
          logger.info("OSGi Bundle Repository: " + bundleRepositories[pos]);
        }
        return new KnopflerfishOSGi(bundleRepositories);
      } catch (MalformedURLException muex) {
        throw new BundleException(muex.getMessage());
      }
    }
  }

}

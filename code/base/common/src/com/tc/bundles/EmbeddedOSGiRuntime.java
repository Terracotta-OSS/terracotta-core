/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.terracottatech.config.Modules;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * For OSGi information please refer to the documentation at the <a href="http://www.osgi.org/">OSGi web page</a>
 */
public interface EmbeddedOSGiRuntime {

  URL[] getRepositories();

  void installBundle(final URL bundle) throws BundleException;

  void installBundles(final URL[] bundles) throws BundleException;

  void startBundles(final URL[] bundles, final EmbeddedOSGiEventHandler handler) throws BundleException;

  void registerService(final Object serviceObject, final Dictionary serviceProps) throws BundleException;

  void registerService(final String serviceName, final Object serviceObject, final Dictionary serviceProps)
      throws BundleException;

  ServiceReference[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException;

  Object getService(final ServiceReference service);

  void ungetService(final ServiceReference service);

  void shutdown();

  static class Factory {

    public static EmbeddedOSGiRuntime createOSGiRuntime(final Modules modules) throws BundleException, Exception {
      List repoList = new ArrayList();
      int repoCount = modules.sizeOfRepositoryArray();
      for (int i = 0; i < repoCount; i++) {
        final String location = modules.getRepositoryArray(i);
        File file = Resolver.resolveRepositoryLocation(location);
        if(file != null) {
          repoList.add(file.toURI().toURL());
        }
      }
      return new KnopflerfishOSGi((URL[]) repoList.toArray(new URL[0]));
    }
  }

}

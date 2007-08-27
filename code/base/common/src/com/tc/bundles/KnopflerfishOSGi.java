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
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;

/**
 * Embedded KnopflerFish OSGi implementation, see the <a href="http://www.knopflerfish.org/">Knopflerfish documentation</a>
 * for more details.
 */
final class KnopflerfishOSGi extends AbstractEmbeddedOSGiRuntime {

  private static final TCLogger logger                        = TCLogging.getLogger(KnopflerfishOSGi.class);

  private static final String   KF_BUNDLESTORAGE_PROP         = "org.knopflerfish.framework.bundlestorage";
  private static final String   KF_BUNDLESTORAGE_PROP_DEFAULT = "memory";

  private final URL[]           repositories;
  private final Framework       framework;

  static {
    System.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, "*");
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
  }

  KnopflerfishOSGi(final URL[] bundleRepositories) throws Exception {
    this.repositories = bundleRepositories;
    System.setProperty("org.knopflerfish.osgi.registerserviceurlhandler", "false");
    framework = new Framework(null);
    framework.launch(0);
  }

  public URL[] getRepositories() {
    return this.repositories;
  }

  public void installBundles(final URL[] locations) throws BundleException {
    for (int i = 0; i < locations.length; i++) {
      installBundle(locations[i]);
    }
  }

  public void startBundles(final URL[] locations, final EmbeddedOSGiEventHandler handler) throws BundleException {
    for (int i = 0; i < locations.length; i++) {
      final long id = framework.getBundleId(locations[i].toString());
      startBundle(id, handler);
    }
  }

  private void startBundle(final long id, final EmbeddedOSGiEventHandler handler) throws BundleException {
    final Bundle bundle = framework.bundles.getBundle(id);
    final boolean isStarting = ((bundle.getState() & Bundle.STARTING) == Bundle.STARTING);
    final boolean isActive = ((bundle.getState() & Bundle.ACTIVE) == Bundle.ACTIVE);

    if (isActive || isStarting) {
      warn(Message.WARN_SKIPPED_ALREADY_ACTIVE, new Object[] { bundle.getSymbolicName() });
      return;
    }

    info(Message.STARTING_BUNDLE, new Object[] { bundle.getSymbolicName() });
    framework.startBundle(bundle.getBundleId());
    info(Message.BUNDLE_STARTED, new Object[] { bundle.getSymbolicName() });
    Assert.assertNotNull(handler);
    handler.callback(bundle);
  }

  private void installBundle(final URL location) throws BundleException {
    try {
      if (logger.isDebugEnabled()) {
        info(Message.INSTALLING_BUNDLE, new Object[] { location });
      }
      final long id = framework.installBundle(location.toString(), location.openStream());
      final Bundle bundle = framework.getSystemBundleContext().getBundle(id);
      final String symname = bundle.getSymbolicName();
      if (symname == null) {
        framework.uninstallBundle(id);
        final File bundleFile = new File(new URI(location.toString()));
        warn(Message.WARN_SKIPPED_FILE_INSTALLATION, new Object[] { bundleFile.getName() });
      } else {
        info(Message.BUNDLE_INSTALLED, new Object[] { symname });
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      exception(Message.ERROR_BUNDLE_INACCESSIBLE, new Object[] { location.toString() }, e);
    }
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

  public void shutdown() {
    if (logger.isDebugEnabled()) {
      info(Message.STOPPING_FRAMEWORK, new Object[0]);
    }
    framework.shutdown();
    info(Message.SHUTDOWN, new Object[0]);
  }

}

/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.jetty_6_1;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.object.config.StandardDSOClientConfigHelper;

public final class JettyConfigurator implements BundleActivator {

  public void start(final BundleContext context) throws Exception {
    final ServiceReference configHelperRef = context.getServiceReference(StandardDSOClientConfigHelper.class.getName());
    if (configHelperRef != null) {
      final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
          .getService(configHelperRef);
      addLoaderAdapters(configHelper);
      context.ungetService(configHelperRef);
    } else {
      throw new BundleException("Expected the " + StandardDSOClientConfigHelper.class.getName()
                                + " service to be registered, was unable to find it");
    }
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  private void addLoaderAdapters(final StandardDSOClientConfigHelper configHelper) {
    // XXX: add loader adapters here
  }

}

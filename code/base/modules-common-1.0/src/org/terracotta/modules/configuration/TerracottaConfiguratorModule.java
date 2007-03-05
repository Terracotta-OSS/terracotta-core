/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.configuration;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.object.config.StandardDSOClientConfigHelper;

public abstract class TerracottaConfiguratorModule implements BundleActivator {

  public void start(final BundleContext context) throws Exception {
    final ServiceReference configHelperRef = context.getServiceReference(StandardDSOClientConfigHelper.class.getName());
    if (configHelperRef != null) {
      final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
          .getService(configHelperRef);
      addInstrumentation(context, configHelper);
      context.ungetService(configHelperRef);
    } else {
      throw new BundleException("Expected the " + StandardDSOClientConfigHelper.class.getName()
          + " service to be registered, was unable to find it");
    }
    registerModuleSpec(context);
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  protected void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
    // default empty body
  }
  
  protected void registerModuleSpec(final BundleContext context) {
    // default empty body
  }


}

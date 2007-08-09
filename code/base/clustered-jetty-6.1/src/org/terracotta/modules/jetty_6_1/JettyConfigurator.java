/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.jetty_6_1;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;
import org.terracotta.modules.jetty_6_1.adapters.ClassPathAdapter;
import org.terracotta.modules.jetty_6_1.adapters.WebAppClassLoaderAdapter;

import com.tc.object.config.IStandardDSOClientConfigHelper;

public final class JettyConfigurator extends TerracottaConfiguratorModule {

  public void start(final BundleContext context) throws Exception {
    final ServiceReference configHelperRef = getConfigHelperReference(context);
    final IStandardDSOClientConfigHelper configHelper = (IStandardDSOClientConfigHelper) context
        .getService(configHelperRef);
    addLoaderAdapters(configHelper);
    context.ungetService(configHelperRef);
  }

  private void addLoaderAdapters(final IStandardDSOClientConfigHelper config) {
    config.addCustomAdapter("org.mortbay.start.Classpath", new ClassPathAdapter());
    config.addCustomAdapter("org.mortbay.jetty.webapp.WebAppClassLoader", new WebAppClassLoaderAdapter());
  }
  
}

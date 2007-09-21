/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.jetty_6_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;
import org.terracotta.modules.jetty_6_1.adapters.ClassPathAdapter;
import org.terracotta.modules.jetty_6_1.adapters.WebAppClassLoaderAdapter;

import com.tc.object.config.StandardDSOClientConfigHelper;

public final class JettyConfigurator extends TerracottaConfiguratorModule {

  protected void addInstrumentation(final BundleContext context) {
    addLoaderAdapters(configHelper);
  }

  private void addLoaderAdapters(final StandardDSOClientConfigHelper config) {
    config.addCustomAdapter("org.mortbay.start.Classpath", new ClassPathAdapter());
    config.addCustomAdapter("org.mortbay.jetty.webapp.WebAppClassLoader", new WebAppClassLoaderAdapter());
  }

}

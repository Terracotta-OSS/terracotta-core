package org.terracotta.modules.websphere_6_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.StandardDSOClientConfigHelper;

public final class WebsphereTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("com.ibm.ws.classloader.JarClassLoader", new JarClassLoaderAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.classloader.ClassGraph", new ClassGraphAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.filter.WebAppFilterManager",
                                  new WebAppFilterManagerClassAdapter());
  }

}

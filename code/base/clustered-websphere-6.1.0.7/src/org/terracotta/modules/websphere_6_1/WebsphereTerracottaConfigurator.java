package org.terracotta.modules.websphere_6_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.IStandardDSOClientConfigHelper;

public final class WebsphereTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context, final IStandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("com.ibm.ws.classloader.JarClassLoader", new JarClassLoaderAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.classloader.ClassGraph", new ClassGraphAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.filter.WebAppFilterManager",
                                  new WebAppFilterManagerClassAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.facade.ServletContextFacade",
                                  new ServletContextFacadeAdapater());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.httpsession.SessionContext", new SessionContextAdapter());
  }

}

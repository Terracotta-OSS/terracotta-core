package org.terracotta.modules.glassfish_1_0_0;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

public class GlassfishTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context) {
    configHelper.addCustomAdapter("com.sun.jdo.api.persistence.model.RuntimeModel", new RuntimeModelAdapter());
    configHelper.addCustomAdapter("com.sun.enterprise.server.PEMain", new PEMainAdapter());
  }

}

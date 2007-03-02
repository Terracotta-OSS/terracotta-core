/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.terracotta.modules.cglib_2_1_3.object.config.CGLibChangeApplicatorSpec;
import org.terracotta.modules.cglib_2_1_3.object.config.CGLibPluginSpec;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ModuleSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;

import java.util.Dictionary;
import java.util.Hashtable;

public final class CGLibTerracottaConfigurator extends TerracottaConfiguratorModule {
  protected final void addInstrumentation(final StandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("net.sf.cglib.proxy.Enhancer", new CGLibProxyEnhancerAdapter());
  }
  
  protected final void registerModuleSpec(final BundleContext context) {
    final Dictionary serviceProps = new Hashtable();
    serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "CGLIB Plugin Spec");
    context.registerService(ModuleSpec.class.getName(), new CGLibPluginSpec(new CGLibChangeApplicatorSpec(getClass().getClassLoader())), serviceProps);
  }

}

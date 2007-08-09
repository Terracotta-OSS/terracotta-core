/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.wicket_1_3_0;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.IStandardDSOClientConfigHelper;

public final class WicketTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context, final IStandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("org.apache.wicket.protocol.http.WebApplication", new WicketWebApplicationAdapter());
  }

}

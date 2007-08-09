/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.struts_1_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.IStandardDSOClientConfigHelper;

public final class StrutsTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected void addInstrumentation(BundleContext context, IStandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("org.apache.struts.taglib.bean.IncludeTag", new IncludeTagAdapter());
    super.addInstrumentation(context, configHelper);
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.struts_1_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;


public final class StrutsTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected void addInstrumentation(BundleContext context) {
    configHelper.addCustomAdapter("org.apache.struts.taglib.bean.IncludeTag", new IncludeTagAdapter());
    super.addInstrumentation(context);
  }

}

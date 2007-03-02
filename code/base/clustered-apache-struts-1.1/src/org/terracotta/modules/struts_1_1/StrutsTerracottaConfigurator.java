/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.struts_1_1;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public final class StrutsTerracottaConfigurator implements BundleActivator {

  private static final String INCLUDE_TAG_CLASSNAME = "org.apache.struts.taglib.bean.IncludeTag";

  public void start(final BundleContext context) throws Exception {
    final ServiceReference configHelperRef = context.getServiceReference(StandardDSOClientConfigHelper.class.getName());
    if (configHelperRef != null) {
      final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
          .getService(configHelperRef);
      addStrutsInstrumentation(configHelper);
      context.ungetService(configHelperRef);
    } else {
      throw new BundleException("Expected the " + StandardDSOClientConfigHelper.class.getName()
          + " service to be registered, was unable to find it");
    }
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  private void addStrutsInstrumentation(final StandardDSOClientConfigHelper configHelper) {
    // Hack for honoring transient in Struts action classes
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("org.apache.struts.action.ActionForm");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec("org.apache.struts.action.ActionMappings");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec("org.apache.struts.action.ActionServletWrapper");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec("org.apache.struts.action.DynaActionFormClass");
    spec.setHonorTransient(true);

    // Hack for Struts <bean:include> tag; when running in tests we need to synchronize as there is a race condition
    // here. The reason for doing this at all is that you can only ever add one custom class adapter for a given name
    synchronized (configHelper) {
      if (!configHelper.hasCustomAdapter(INCLUDE_TAG_CLASSNAME)) {
        configHelper.addCustomAdapter(INCLUDE_TAG_CLASSNAME, new IncludeTagAdapter());
      }
    }
  }

}

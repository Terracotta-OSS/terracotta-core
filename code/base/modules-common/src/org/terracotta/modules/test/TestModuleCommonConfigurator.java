/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

/**
 * This exists to test exported class feature
 * See {@link com.tctest.BundleClassExportTest}
 */
public class TestModuleCommonConfigurator extends TerracottaConfiguratorModule {
  protected void addInstrumentation(final BundleContext context) {
    System.err.println("@@@ in TestModuelCommonConfigurator:");
    Thread.currentThread().dumpStack();
    Bundle thisBundle = getExportedBundle(context, getExportedBundleName());
    addExportedBundleClass(thisBundle, DummyClass.class.getName());
  }
  
  protected String getExportedBundleName() {
    return "org.terracotta.modules.modules-common";
  }
}

/*
 *
 */
package org.terracotta.modules.surefire_2_3;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;

/**
 * @author Eugene Kuleshov
 */
public class SurefireTerracottaConfigurator extends TerracottaConfiguratorModule {

  private static final String CLUSTERED_SUREFIRE_BUNDLE_NAME = "org.terracotta.modules.clustered-surefire-2.3";

  protected void addInstrumentation(BundleContext context) {
    configHelper.addCustomAdapter("junit.framework.TestSuite", new JUnitTestSuiteAdapter());
    configHelper.addCustomAdapter("org.apache.maven.surefire.booter.IsolatedClassLoader",
                                  new IsolatedClassLoaderAdapter());

    configHelper.addIncludePattern(JUnitTestSuiteAdapter.CLUSTERED_JUNIT_BARRIER_CLASS, true);
    configHelper.addAutolock("* " + JUnitTestSuiteAdapter.CLUSTERED_JUNIT_BARRIER_CLASS + ".*(..)",
                             ConfigLockLevel.WRITE);

    Bundle bundle = getExportedBundle(context, CLUSTERED_SUREFIRE_BUNDLE_NAME);
    if (null == bundle) {
      String msg = "Couldn't find bundle with symbolic name '" + CLUSTERED_SUREFIRE_BUNDLE_NAME + "'"
                   + " during the instrumentation configuration of the bundle '"
                   + context.getBundle().getSymbolicName() + "'";
      throw new RuntimeException(msg);
    }

    addExportedBundleClass(bundle, JUnitTestSuiteAdapter.CLUSTERED_JUNIT_BARRIER_CLASS);
  }

}

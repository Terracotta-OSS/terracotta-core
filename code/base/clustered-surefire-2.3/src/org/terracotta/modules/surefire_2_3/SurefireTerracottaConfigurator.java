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

  private static final String CLUSTERED_SUREFIRE_BUNDLE_NAME = "org.terracotta.modules.clustered_surefire_2.3";

  protected void addInstrumentation(BundleContext context) {
    configHelper.addCustomAdapter("junit.framework.TestSuite", new JUnitTestSuiteAdapter());
    configHelper.addCustomAdapter("org.apache.maven.surefire.booter.IsolatedClassLoader", new IsolatedClassLoaderAdapter());
    
    configHelper.addIncludePattern(JUnitTestSuiteAdapter.CLUSTERED_JUNIT_BARRIER_CLASS, true);
    configHelper.addAutolock("* " + JUnitTestSuiteAdapter.CLUSTERED_JUNIT_BARRIER_CLASS + ".*(..)", ConfigLockLevel.WRITE);

    // find the bundle that contains the replacement classes
    Bundle[] bundles = context.getBundles();
    Bundle bundle = null;
    for (int i = 0; i < bundles.length; i++) {
      if (CLUSTERED_SUREFIRE_BUNDLE_NAME.equals(bundles[i].getSymbolicName())) {
        bundle = bundles[i];
        break;
      }
    }
    
    if(bundle!=null) {
      addExportedBundleClass(bundle, JUnitTestSuiteAdapter.CLUSTERED_JUNIT_BARRIER_CLASS);
    }
  }
  
}

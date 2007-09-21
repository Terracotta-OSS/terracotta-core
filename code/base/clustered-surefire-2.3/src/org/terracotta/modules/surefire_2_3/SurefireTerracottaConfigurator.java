/*
 *
 */
package org.terracotta.modules.surefire_2_3;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;

/**
 * @author Eugene Kuleshov
 */
public class SurefireTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected void addInstrumentation(BundleContext context) {
    configHelper.addCustomAdapter("junit.framework.TestSuite", new JUnitTestSuiteAdapter());
    configHelper.addCustomAdapter("org.apache.maven.surefire.booter.IsolatedClassLoader", new IsolatedClassLoaderAdapter());
    
    configHelper.addIncludePattern("EDU.oswego.cs.dl.util.concurrent.CyclicBarrier", true);
    configHelper.addAutolock("* EDU.oswego.cs.dl.util.concurrent.CyclicBarrier.*(..)", ConfigLockLevel.WRITE);
  }
  
}

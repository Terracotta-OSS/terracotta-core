/*
 *
 */
package org.terracotta.modules.surefire_2_3;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.StandardDSOClientConfigHelper;

/**
 * @author Eugene Kuleshov
 */
public class SurefireTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper config) {
    config.addCustomAdapter("junit.framework.TestSuite", new JUnitTestSuiteAdapter());
    config.addCustomAdapter("org.apache.maven.surefire.booter.IsolatedClassLoader", new IsolatedClassLoaderAdapter());
    
    config.addIncludePattern("EDU.oswego.cs.dl.util.concurrent.CyclicBarrier", true);
    config.addAutolock("* EDU.oswego.cs.dl.util.concurrent.CyclicBarrier.*(..)", ConfigLockLevel.WRITE);
  }
  
}

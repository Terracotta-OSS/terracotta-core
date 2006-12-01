/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package launch;


import java.io.File;
import java.io.PrintStream;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

public final class JDKEnvironment {

  public static final JDKEnvironment OSGi_MIN_1_0 = new JDKEnvironment("OSGi/Minimum-1.0");
  public static final JDKEnvironment OSGi_MIN_1_1 = new JDKEnvironment("OSGi/Minimum-1.1");

  public static final JDKEnvironment CDC_1_0      = new JDKEnvironment("CDC-1.0/Foundation-1.0");
  public static final JDKEnvironment CDC_1_1      = new JDKEnvironment("CDC-1.1/Foundation-1.1");

  public static final JDKEnvironment JRE_1_1      = new JDKEnvironment("JRE-1.1");
  public static final JDKEnvironment J2SE_1_2     = new JDKEnvironment("J2SE-1.2");
  public static final JDKEnvironment J2SE_1_3     = new JDKEnvironment("J2SE-1.3");
  public static final JDKEnvironment J2SE_1_4     = new JDKEnvironment("J2SE-1.4");
  public static final JDKEnvironment J2SE_1_5     = new JDKEnvironment("J2SE-1.5");
  public static final JDKEnvironment JavaSE_1_6   = new JDKEnvironment("JavaSE-1.6");

  private final String               environment;

  private JDKEnvironment(final String environment) {
    this.environment = environment;
  }

  public File getJavaHome() {
    final IExecutionEnvironmentsManager envManager = JavaRuntime.getExecutionEnvironmentsManager();
    final IExecutionEnvironment jdkEnv = envManager.getEnvironment(environment);
    if (jdkEnv == null) {
      return null;
    } else {
      final IVMInstall jdk = jdkEnv.getDefaultVM();
      if (jdk == null) {
        IVMInstall[] allInstalls = jdkEnv.getCompatibleVMs();
        if (allInstalls == null || allInstalls.length == 0) {
          return null;
        } else {
          return allInstalls[0].getInstallLocation().getAbsoluteFile();
        }
      } else {
        return jdk.getInstallLocation().getAbsoluteFile();
      }
    }
  }

  public static void dumpJDKs(final PrintStream output) {
    final IExecutionEnvironmentsManager envManager = JavaRuntime.getExecutionEnvironmentsManager();
    final IExecutionEnvironment[] jdkEnvs = envManager.getExecutionEnvironments();
    if (jdkEnvs == null || jdkEnvs.length == 0) {
      output.println("No JVM environments available");
    } else {
      for (int pos = 0; pos < jdkEnvs.length; ++pos) {
        final IExecutionEnvironment env = jdkEnvs[pos];
        output.println("Environment[" + env.getId() + "/" + env.getDescription() + "]:");
        final IVMInstall defaultVM = env.getDefaultVM();
        if (defaultVM != null) {
          output.println("\tDefault VM: " + defaultVM.getId() + " installed at "
                         + defaultVM.getInstallLocation().getAbsolutePath());
        } else {
          output.println("\tNo default VM for this environment");
        }
        final IVMInstall[] installs = env.getCompatibleVMs();
        for (int installPos = 0; installPos < installs.length; ++installPos) {
          final IVMInstall install = installs[installPos];
          output.println("\tCompatible VM: " + install.getId() + " installed at "
                         + install.getInstallLocation().getAbsolutePath());
        }
      }
    }
  }

}

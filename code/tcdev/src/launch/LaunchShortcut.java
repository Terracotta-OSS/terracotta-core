/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import launch.actions.WorkbenchOptionAction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchDescription;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.console.IOConsoleOutputStream;

import refreshall.Activator;

public class LaunchShortcut extends JUnitLaunchShortcut implements IJavaLaunchConfigurationConstants {

  private static final String         Jdk14Home           = JDKEnvironment.J2SE_1_4.getJavaHome().getAbsolutePath();
  private static final String         Jdk15Home           = JDKEnvironment.J2SE_1_5.getJavaHome().getAbsolutePath();

  private static final String         TESTS_PREP_PROP_LOC = "common" + File.separator + "build.eclipse"
                                                            + File.separator + "tests.base.classes" + File.separator
                                                            + "tests-prepared.properties";
  // private static final String BUILD_PATH = "";
  private static final String         VM_ARGS_COUNT       = "tcbuild.prepared.jvmargs";
  private static final String         VM_ARG              = "tcbuild.prepared.jvmarg_";
  private static final String         SYS_PROP_PREFIX     = "tcbuild.prepared.system-property.";
  private static final String         JVM_VERSION         = "tcbuild.prepared.jvm.version";
  private static final String         TCBUILD             = "tcbuild";
  private static final String         CHECK_PREP          = "check_prep";
  // private static final String BUILD_WITH_14_OPTION = "run-1.4-tests-with-1.5=";
  private static final byte[]         NEWLINE             = "\n".getBytes();

  private final IOConsoleOutputStream console;
  private Properties                  argTypes;

  public LaunchShortcut() {
    console = Activator.getDefault().getConsoleStream();
  }

  protected void searchAndLaunch(Object[] search, String mode) {
    if (search != null && search.length > 0 && search[0] instanceof IJavaElement) {
      try {
        tcCheckPrep((IJavaElement) search[0]);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
    super.searchAndLaunch(search, mode);
  }

  public void tcCheckPrep(IJavaElement element) throws Exception {
    println("Running: tcCheckPrep()");
    String relativePath = element.getPath().toString();
    String absolutePath = element.getResource().getLocation().toString();
    String basePath = absolutePath.substring(0, absolutePath.length() - relativePath.length() + 1);
    File prepProps = new File(basePath + TESTS_PREP_PROP_LOC);
    String[] parts = relativePath.split("/");
    String module = parts[1];
    String subtree = parts[2];
    File wkDir = new File(basePath);
    if (!prepProps.exists()) runCheckPrep(wkDir, module, subtree, basePath);
    loadProperties(prepProps, module, subtree, wkDir, basePath);
  }

  private void loadProperties(File prepProps, String module, String subtree, File wkDir, String basePath)
      throws Exception {
    Properties properties = new Properties();
    properties.load(new FileInputStream(prepProps));
    if (!validatePrep(properties, module, subtree)) runCheckPrep(wkDir, module, subtree, basePath);
    argTypes = properties;
  }

  public ILaunchConfiguration findOrCreateLaunchConfiguration(String mode, JUnitLaunchShortcut registry,
                                                              JUnitLaunchDescription description)
      throws LaunchCancelledByUserException {
    if (argTypes == null) throw new RuntimeException(
                                                     "vmArgs null, this should never happen. JUnit impl must have changed.");

    try {
      ILaunchConfigurationWorkingCopy wc = super.findOrCreateLaunchConfiguration(mode, registry, description)
          .getWorkingCopy();

      setInstalledJRE(argTypes.getProperty(JVM_VERSION), wc);

      StringBuffer vmArgs = new StringBuffer();
      int argsCount = new Integer(argTypes.getProperty(VM_ARGS_COUNT)).intValue();
      for (int i = 0; i < argsCount; i++) {
        vmArgs.append(argTypes.getProperty(VM_ARG + i) + " ");
      }
      Enumeration enumx = argTypes.propertyNames();
      while (enumx.hasMoreElements()) {
        String key = (String) enumx.nextElement();
        if (key.startsWith(SYS_PROP_PREFIX)) {
          String value = (String) argTypes.getProperty(key);
          vmArgs.append("-D" + key.substring(SYS_PROP_PREFIX.length(), key.length()) + "=" + value + " ");
        }
      }
      if (vmArgs.length() > 0) wc.setAttribute(ATTR_VM_ARGUMENTS, vmArgs.toString());

      return wc.doSave();

    } catch (CoreException ce) {
      JUnitPlugin.log(ce);
      println(ce.getMessage());
    }

    throw new RuntimeException();
  }

  private void runCheckPrep(File wkDir, String module, String subtree, String basePath) throws Exception {
    if (!subtree.startsWith("tests.")) throw new IllegalArgumentException("Subtree must start with \"tests.\"");
    if (!(subtree.endsWith("unit") || subtree.endsWith("system"))) throw new IllegalArgumentException(
                                                                                                      "Subtree must end with \"unit\" or \"system\"");

    boolean buildWith15 = true;
    Preferences prefs = Activator.getDefault().getPluginPreferences();
    if (prefs.getBoolean(WorkbenchOptionAction.KEY)) buildWith15 = false;
    int increment = 0;
    String[] commandLine = new String[4];

    // windows hack
    if (((String) System.getProperty("os.name")).toLowerCase().indexOf("win") != -1) {
      commandLine = new String[commandLine.length + 2];
      commandLine[increment++] = "cmd";
      commandLine[increment++] = "/c";
    }
    commandLine[increment++] = basePath + TCBUILD; // BUILD_PATH + File.separator +
    commandLine[increment++] = CHECK_PREP;
    // commandLine[increment++] = BUILD_WITH_14_OPTION + buildWith15;
    commandLine[increment++] = module;
    commandLine[increment++] = subtree.substring("tests.".length(), subtree.length());

    Map env = System.getenv();
    Map modifiedEnv = new HashMap(env);
    modifiedEnv.put("JAVA_HOME", Jdk15Home);
    modifiedEnv.put("TC_JAVA_HOME_14", Jdk14Home);
    modifiedEnv.put("TC_JAVA_HOME_15", Jdk15Home);
    String[] environment = new String[modifiedEnv.size()];
    Iterator iter = modifiedEnv.entrySet().iterator();
    Map.Entry entry;
    for (int i = 0; iter.hasNext(); i++) {
      entry = (Map.Entry) iter.next();
      environment[i] = entry.getKey() + "=" + entry.getValue();
    }

    StringBuffer comStr = new StringBuffer();
    for (int i = 0; i < commandLine.length; i++) {
      comStr.append(" ");
      comStr.append(commandLine[i]);
    }
    println("****************************** EXECUTING TC_BUILD ******************************\n" + "$" + comStr + "\n");

    Runtime runtime = Runtime.getRuntime();
    Process process = runtime.exec(commandLine, environment, wkDir);
    writeInput(process.getErrorStream(), process.getInputStream());
    if (process.waitFor() != 0) throw new Exception("build failed");
  }

  private void writeInput(final InputStream err, final InputStream out) {
    Thread errWriter = new Thread() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(err));

      public void run() {
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            println(line);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          try {
            reader.close();
          } catch (IOException e) {
            // oh well
          }
        }
      }
    };
    Thread outWriter = new Thread() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(out));

      public void run() {
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            println(line);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          try {
            reader.close();
          } catch (IOException e) {
            // oh well
          }
        }
      }
    };
    errWriter.start();
    outWriter.start();
  }

  private void setInstalledJRE(String jreVersion, ILaunchConfigurationWorkingCopy wc)
      throws LaunchCancelledByUserException {

    IVMInstall2 currInstall = (IVMInstall2) JavaRuntime.getDefaultVMInstall();
    if (jreVersion.startsWith(currInstall.getJavaVersion())) return;

    boolean jreAvailable = false;
    IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
    for (int i = 0; i < installTypes.length; i++) {
      IVMInstall[] installs = installTypes[i].getVMInstalls();
      for (int j = 0; j < installs.length; j++) {
        if (installs[j] instanceof IVMInstall2) {
          IVMInstall2 install2 = (IVMInstall2) installs[j];
          if (jreVersion.startsWith(install2.getJavaVersion())) {
            wc.setAttribute(ATTR_JRE_CONTAINER_PATH, JavaRuntime.newJREContainerPath(installs[j]).toPortableString());
            jreAvailable = true;
          }
        }
      }
    }
    if (!jreAvailable) {
      println("Java Version: " + jreVersion + " not available as an installed jre in Eclipse.");
      throw new LaunchCancelledByUserException();
    } else {
      println("Using JRE Version: " + jreVersion);
    }
  }

  private boolean validatePrep(Properties properties, String module, String subtree) {
    if (!properties.getProperty("tcbuild.prepared.module", "").equals(module)) return false;
    if (!properties.getProperty("tcbuild.prepared.subtree", "").equals(subtree)) return false;
    Preferences prefs = Activator.getDefault().getPluginPreferences();
    String jreVersion = properties.getProperty(JVM_VERSION);
    if ((jreVersion.indexOf("1.4") != -1) && (prefs.getBoolean(WorkbenchOptionAction.KEY))) { return false; }
    return true;
  }

  private void println(final String line) {
    try {
      synchronized (console) {
        console.write(line);
        console.write(NEWLINE);
        console.flush();
      }
    } catch (IOException ioe) {
      synchronized (System.err) {
        System.err.println("Unable to send line to console --> " + line);
        System.err.flush();
      }
    }
  }

}

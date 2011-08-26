/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall3;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

import com.tc.object.tools.BootJarSignature;
import com.tc.object.tools.UnsupportedVMException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class BootJarHelper implements IJavaLaunchConfigurationConstants {
  private static BootJarHelper m_helper;
  
  private static final String LAUNCH_LABEL =
    "DSO BootJar Namer";

  private static final String CLASSPATH_PROVIDER =
    "org.terracotta.dso.classpathProvider";
  
  private static final String BOOT_JAR_NAMER =
    "com.tc.object.tools.BootJarSignature";
  
  public static synchronized BootJarHelper getHelper() {
    if(m_helper == null) {
      m_helper = new BootJarHelper();
    }
    return m_helper;
  }
  
  private BootJarHelper() {
    super();
  }

  /**
   * Retrieve the name of the default bootjar.
   */
  public String getBootJarName() throws CoreException {
    return getBootJarName((String)null);
  }
  
  /**
   * Retrieve the name of the default bootjar.
   */
  public String getBootJarName(String jreContainerPath) throws CoreException {
    ILaunchManager           manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type    = manager.getLaunchConfigurationType(ID_JAVA_APPLICATION);
    ILaunchConfiguration[]   configs = manager.getLaunchConfigurations(type);
    
    for(int i = 0; i < configs.length; i++) {
      ILaunchConfiguration config = configs[i];
      
      if(config.getName().equals(LAUNCH_LABEL)) {
        config.delete();
        break;
      }
    }
    
    ILaunchConfigurationWorkingCopy wc       = type.newInstance(null, LAUNCH_LABEL);
    String                          runMode  = ILaunchManager.RUN_MODE;
    JavaLaunchDelegate              delegate = new JavaLaunchDelegate();
    Launch                          launch   = new Launch(wc, runMode, null);

    wc.setAttribute(ATTR_CLASSPATH_PROVIDER, CLASSPATH_PROVIDER);
    wc.setAttribute(ATTR_MAIN_TYPE_NAME, BOOT_JAR_NAMER);
    
    if(jreContainerPath != null) {
      wc.setAttribute(ATTR_JRE_CONTAINER_PATH, jreContainerPath);
    }
    
    delegate.launch(wc, runMode, launch, null);
    
    IProcess       process      = launch.getProcesses()[0];
    IStreamsProxy  streamsProxy = process.getStreamsProxy();
    IStreamMonitor outMonitor   = streamsProxy.getOutputStreamMonitor();    
    
    while(!process.isTerminated()) {
      ThreadUtil.reallySleep(100);
    }
    
    return outMonitor.getContents().trim();
  }
  
  /**
   * Retrieve the bootjar path.
   */
  public IPath getBootJarPath() throws CoreException {
    return getBootJarPath(getBootJarName());
  }
  
  /**
   * Retrieve the bootjar path for the current VM.
   */
  public IPath getBootJarPathForThisVM() throws UnsupportedVMException {
    return getBootJarPath(BootJarSignature.getBootJarNameForThisVM());
  }

  /**
   * Retrieve the bootjar path given the bootjar name.
   */
  public IPath getBootJarPath(String bootJarName) {
    IPath libDirPath  = TcPlugin.getDefault().getLibDirPath();
    IPath bootJarPath = libDirPath.append("dso-boot").append(bootJarName);
    
    return bootJarPath;
  }

  public File getBootJarFileForThisVM() throws UnsupportedVMException {
    return getBootJarPathForThisVM().toFile();
  }
  
  /**
   * Retrieve the bootjar file.
   */
  public File getBootJarFile() throws CoreException {
    return getBootJarPath().toFile();
  }
  
  public String getBootJarName(IJavaProject javaProject) {
    try {
      IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
      if (vmInstall == null) vmInstall = JavaRuntime.getDefaultVMInstall();
      if (vmInstall instanceof IVMInstall3) {
        String[] props = { "java.version", "java.vendor", "java.runtime.version", "java.vm.name", "os.name" };
        Map sysProps = ((IVMInstall3) vmInstall).evaluateSystemProperties(props, new NullProgressMonitor());
        Properties properties = new Properties();
        properties.putAll(sysProps);
        return BootJarSignature.getBootJarName(properties);
      }
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
    return null;
  }
}

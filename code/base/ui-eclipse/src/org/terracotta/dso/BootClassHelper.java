/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.tc.object.tools.BootJar;
import com.tc.object.tools.BootJarSignature;

import java.io.File;
import java.util.Set;

/**
 * Utility for determining if a type is a pre-instrumented class in the bootjar. 
 */

public class BootClassHelper {
  private static BootClassHelper m_helper;
  private Set                    m_bootClasses;
  
  public static BootClassHelper getHelper() {
    return m_helper;    
  }
  
  static BootClassHelper initialize() throws Exception {
    return m_helper = new BootClassHelper();
  }

  /*
   * Initialize from the boot-jar that would be used by the VM
   * Eclipse is running in. Eclipse can be running in one VM version
   * while the user can specify that internally it use another
   * version. If that is the case, BootJar.getBootJarForReading will
   * fail complaining that we're trying to read an incompatible
   * version.
   */
  public BootClassHelper() throws Exception {
    TcPlugin plugin     = TcPlugin.getDefault();
    IPath    libDirPath = plugin.getLibDirPath();
    
    if(libDirPath.append("tc.jar").toFile().exists()) {
      String  bootJarName = BootJarSignature.getBootJarNameForThisVM();
      IPath   bootJarPath = libDirPath.append("dso-boot").append(bootJarName);
      File    bootJarFile = bootJarPath.toFile();
      BootJar bootJar     = BootJar.getBootJarForReading(bootJarFile);
    
      m_bootClasses = bootJar.getAllPreInstrumentedClasses();
    }
  }

  public boolean isAdaptable(final ICompilationUnit module) {
    return module != null ? isAdaptable(module.findPrimaryType()) : false;
  }
  
  public boolean isAdaptable(final IClassFile classFile) {
    try {
      return classFile != null ? isAdaptable(classFile.getType()) : false;
    } catch(JavaModelException jme) {
      return false;
    }
  }
  
  public boolean isAdaptable(final IType type) {
    return type != null ? isAdaptable(PatternHelper.getFullyQualifiedName(type)) : false;
  }
  
  public boolean isAdaptable(String fullName) {
    return m_bootClasses != null && m_bootClasses.contains(fullName);
  }
}

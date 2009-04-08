/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import com.tc.object.LiteralValues;
import com.tc.object.tools.BootJar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility for determining if a type is a pre-instrumented class in the bootjar.
 */

public class BootClassHelper {
  private Set<String>          m_bootClasses = new HashSet<String>();

  public static String getBootJarName(IJavaProject javaProject) {
    String bootJarName = BootJarHelper.getHelper().getBootJarName(javaProject);
    if (bootJarName != null) return bootJarName;

    try {
      IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(javaProject);
      String jreEntryPath = null;

      if (jreEntry != null) {
        IPath jrePath = jreEntry.getPath(); 
        if (jrePath != null) {
          jreEntryPath = jrePath.makeAbsolute().toPortableString();
        }
      }
      bootJarName = BootJarHelper.getHelper().getBootJarName(jreEntryPath);
    } catch (Exception e) {
      TcPlugin.getDefault().openError("Initializing BootClassHelper", e);
    }
    return bootJarName;
  }

  /**
   * Return if there exists either an actual bootjar in javaProject or a cache file that
   * matches the current VM.
   * 
   * This is used (1) by the ProjectWizard to determine if a bootjar should be created
   * when the nature is added, and (2) by ResourceDeltaVisitor when the classpath changes,
   * possibly due to the VM being changed.
   */
  public static boolean canGetBootTypes(IJavaProject javaProject) {
    String bootJarName = getBootJarName(javaProject);
    File bootJarFile = javaProject.getProject().getLocation().append(bootJarName).toFile();
    File bootTypesCacheFile = getBootTypesCacheFile(bootJarName);
      
    return (bootJarFile.exists() || bootTypesCacheFile.exists());
  }
  
  /*
   * Initialize from the boot-jar that would be used by the VM Eclipse is running in. Eclipse can be running in one VM
   * version while the user can specify that internally it use another version. If that is the case,
   * BootJar.getBootJarForReading will fail complaining that we're trying to read an incompatible version.
   */
  public BootClassHelper(IJavaProject javaProject) {
    this(javaProject, getBootJarName(javaProject));
  }

  public static File getBootTypesCacheFile(String bootJarName) {
    return TcPlugin.getDefault().getStateLocation().append(bootJarName+".cache").toFile();
  }
  
  public BootClassHelper(IJavaProject javaProject, String bootJarName) {
    if (bootJarName == null) return;
    try {
      // first try to load boot types from cache
      File bootCacheFile = getBootTypesCacheFile(bootJarName);
      if(bootCacheFile.exists()) {
        FileReader fReader = new FileReader(bootCacheFile);
        BufferedReader bufReader = new BufferedReader(fReader);
        String s;
        
        while((s = bufReader.readLine()) != null) {
          m_bootClasses.add(s);
        }
        return;
      }

      // next see if there's a bootjar in the project
      File bootJarFile = javaProject.getProject().getLocation().append(bootJarName).toFile();
      if (bootJarFile.exists()) {
        JarFile jarFile = new JarFile(bootJarFile, false);
        for (Enumeration e = jarFile.entries(); e.hasMoreElements();) {
          JarEntry entry = (JarEntry) e.nextElement();
          String entryName = entry.getName();
          if (entryName.endsWith(".class") && !entryName.startsWith("com/tc")) {
            entryName = entryName.substring(0, entryName.lastIndexOf('.')).replace('/', '.');
            if(!isForeign(entry)) {
              m_bootClasses.add(entryName);
            }
          }
        }
        jarFile.close();
      }
    } catch (Exception e) {
      TcPlugin.getDefault().openError("Initializing BootClassHelper", e);
    }
  }

  /**
   * Write the types names found in bootJarFile to the cache in the plugin state area.
   */
  public static void cacheBootTypes(File bootJarFile) {
    File cacheFile = BootClassHelper.getBootTypesCacheFile(bootJarFile.getName());
    FileWriter fWriter = null;
    JarFile jarFile = null;

    try {
      jarFile = new JarFile(bootJarFile, false);
      fWriter = new FileWriter(cacheFile);
      String nl = System.getProperty("line.separator");
      for (Enumeration e = jarFile.entries(); e.hasMoreElements();) {
        JarEntry entry = (JarEntry) e.nextElement();
        String entryName = entry.getName();
        if (entryName.endsWith(".class") && !entryName.startsWith("com/tc")) {
          entryName = entryName.substring(0, entryName.lastIndexOf('.')).replace('/', '.');
          if(!BootClassHelper.isForeign(entry)) {
            fWriter.write(entryName);
            fWriter.write(nl);
          }
        }
      }
    } catch (Exception e) {
      TcPlugin.getDefault().openError("Cacheing boot types", e);
    } finally {
      if (fWriter != null) try {
        fWriter.close();
      } catch (Exception ignore) {/**/
      }
      if (jarFile != null) try {
        jarFile.close();
      } catch (Exception ignore) {/**/
      }
    }
  }

  private static String getJarEntryAttributeValue(JarEntry entry, String attributeName) throws IOException {
    Attributes attributes = entry.getAttributes();
    if (attributes == null) throw new AssertionError("Invalid jar file: No attributes for jar entry: "
                                                     + entry.getName());
    String value = attributes.getValue(attributeName);
    if (value == null) throw new AssertionError("Invalid jar file: No " + attributeName + " attribute for jar entry: "
                                                + entry.getName());
    return value;
  }

  public static boolean isForeign(JarEntry entry) throws IOException {
    return Boolean.valueOf(getJarEntryAttributeValue(entry, BootJar.FOREIGN_NAME)).booleanValue();
  }
  
  public boolean isAdaptable(final ICompilationUnit module) {
    return module != null ? isAdaptable(module.findPrimaryType()) : false;
  }

  public boolean isAdaptable(final IClassFile classFile) {
    try {
      return classFile != null ? isAdaptable(classFile.getType()) : false;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isAdaptable(final IType type) {
    if (type != null) { return isAdaptable(PatternHelper.getFullyQualifiedName(type)); }
    return false;
  }

  public boolean isAdaptable(String fullName) {
    return LiteralValues.isLiteral(fullName) || (m_bootClasses != null && m_bootClasses.contains(fullName));
  }
}

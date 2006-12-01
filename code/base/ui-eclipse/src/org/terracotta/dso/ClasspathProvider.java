/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ClasspathProvider extends StandardClasspathProvider {
  public ClasspathProvider() {
    super();
  }
  
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration)
    throws CoreException
  {
    IRuntimeClasspathEntry[] cpe     = super.computeUnresolvedClasspath(configuration);
    IPath                    jarPath = TcPlugin.getDefault().getLibDirPath().append("tc.jar"); 
    
    if(jarPath.toFile().exists()) {
      IRuntimeClasspathEntry[] result = new IRuntimeClasspathEntry[cpe.length+1];
      
      System.arraycopy(cpe, 0, result, 0, cpe.length);
      result[cpe.length] = JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath);

      return result;
    }
    else {
      ArrayList list  = new ArrayList(Arrays.asList(cpe));
      IPath[]   paths = gatherDevClasspathEntries();
      
      for(int i = 0; i < paths.length; i++) {
        list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(paths[i]));
      }
  
      return (IRuntimeClasspathEntry[])list.toArray(new IRuntimeClasspathEntry[0]);
    }
  }
  
  public static String makeDevClasspath() {
    IPath[]      paths = gatherDevClasspathEntries();
    StringBuffer sb    = new StringBuffer();
    String       sep   = System.getProperty("path.separator");
    
    for(int i = 0; i < paths.length; i++) {
      if(i > 0) sb.append(sep);
      sb.append(paths[i].toOSString());
    }
    
    return sb.toString();
  }
  
  private static IPath[] gatherDevClasspathEntries() {
    IPath location  = TcPlugin.getDefault().getLocation();
    List  list      = new ArrayList();
    IPath buildPath = location.append("..");

    String[] dirs = {
      "deploy",
      "common",
      "management",
      "aspectwerkz",
      "thirdparty",
      "dso-common",
      "dso-common-jdk15",
      "dso-l1",
      "dso-l1-jdk15",
      "dso-l2"
    };
    
    for(int i = 0; i < dirs.length; i++) {
      list.add(buildPath.append(dirs[i]).append("build.eclipse").append("src.classes"));
    }
    
    String[] extensions = new String[] {"jar"};
    List     fileList   = new ArrayList();
    File     libDir;
    
    for(int i = 0; i < dirs.length; i++) {
      libDir = location.append("..").append(dirs[i]).append("lib").toFile();

      if(libDir.exists()) {
        fileList.addAll(FileUtils.listFiles(libDir, extensions, false));
      }
    }
    
    Iterator fileIter = fileList.iterator();
    File     file;
    
    while(fileIter.hasNext()) {
      file = (File)fileIter.next();
      list.add(new Path(file.getAbsolutePath()));
    }
    
    return (IPath[])list.toArray(new IPath[list.size()]);
  }
}

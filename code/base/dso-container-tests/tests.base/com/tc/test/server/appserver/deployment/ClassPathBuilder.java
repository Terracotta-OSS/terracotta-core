/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;


import java.io.File;
import java.io.FilenameFilter;

public class ClassPathBuilder {

  StringBuffer sb = new StringBuffer();
  
  public void addDir(String dir) {
    FileSystemPath dirPath = FileSystemPath.existingDir(dir);
    addFileOrDir(dirPath);
  }

  private void addFileOrDir(FileSystemPath dirPath) {
    if (sb.length() > 0) sb.append(";");
    sb.append(dirPath);
  }

  public String makeClassPath() {
    return sb.toString();
  }

  public void addJARsInDir(String dirContainingJARS) {
    FileSystemPath dirPath = FileSystemPath.existingDir(dirContainingJARS);
    String[] jars = dirPath.getFile().list(new FilenameFilter(){

      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }});
    for (int i = 0; i < jars.length; i++) {
      String jar = jars[i];
      addFileOrDir(dirPath.existingFile(jar));
    }
    
  }


}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.File;
import java.io.FileOutputStream;

/**
 * A little utility class that will write class files to disk.
 */
public abstract class AbstractClassDumper {

  private final File adaptedRoot = getFileRoot();

  public synchronized void write(String name, byte[] b) {
    if (adaptedRoot == null) { return; }

    name = name.replace('.', '/') + ".class";
    FileOutputStream fos = null;

    try {
      try {
        String pattern = File.separator.replaceAll("\\\\", "\\\\\\\\");
        String[] strings = new File(adaptedRoot, name).getAbsolutePath().split(pattern);

        final StringBuffer sb;
        if (adaptedRoot.getAbsolutePath().startsWith("/")) {
          sb = new StringBuffer("/");
        } else {
          sb = new StringBuffer();
        }

        for (int i = 0; i < strings.length - 1; i++) {
          sb.append(strings[i]);
          sb.append(File.separatorChar);
        }

        File dir = new File(sb.toString());
        if (!dir.exists()) {
          dir.mkdirs();
        }

        File outFile = new File(adaptedRoot, name);
        System.out.println("Writing resource: " + outFile);
        System.out.flush();
        fos = new FileOutputStream(outFile);
        fos.write(b);
      } finally {
        if (fos != null) {
          fos.close();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private File getFileRoot() {
    try {
      boolean writeToDisk = (System.getProperty(getPropertyName()) != null);
      if (!writeToDisk) { return null; }

      String userHome = System.getProperty("user.home");

      if (userHome != null) {
        File homeDir = new File(userHome);
        if (homeDir.isDirectory() && homeDir.canWrite()) { return new File(homeDir, getDumpDirectoryName()); }
      }

      return null;
    } catch (Exception e) {
      // you can get a SecurityException here, but we shouldn't blow up just b/c of that
      e.printStackTrace();
      return null;
    }
  }
  
  protected abstract String getDumpDirectoryName();
  protected abstract String getPropertyName();
}

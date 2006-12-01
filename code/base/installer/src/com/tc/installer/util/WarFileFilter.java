/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.installer.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WarFileFilter implements FileFilter {

  public boolean accept(File file) {
    if (!file.canRead()) return false;
    if (!file.isFile()) return false;
    if (!Pattern.matches(".*\\.war", file.getName().toLowerCase())) return false;
    return isValidWarFormat(file);
  }
  
  private boolean isValidWarFormat(File file) {
    try {
      ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
      ZipEntry entry = null;
      while ((entry = zin.getNextEntry()) != null) {
        if (entry.getName().equals("WEB-INF/")) return true;
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }
  
}

/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class FileUtils {

  /*
   * deletes all files with matching extension. Does not recurse into sub directories.
   */
  public static void forceDelete(File directory, String extension) throws IOException {
    Iterator files = org.apache.commons.io.FileUtils.iterateFiles(directory, new String[] { extension }, false);
    while (files.hasNext()) {
      File f = (File) files.next();
      org.apache.commons.io.FileUtils.forceDelete(f);
    }
  }

  /**
   * copy one file to another. Can also copy directories
   */
  public static void copyFile(File src, File dest) throws IOException {
    if (src.isDirectory()) {
      dest.mkdirs();
      String list[] = src.list();
      for (int i = 0; i < list.length; i++) {
        copyFile(new File(src.getAbsolutePath(), list[i]), new File(dest.getAbsolutePath(), list[i]));
      }
    } else {
      FileInputStream fin = new FileInputStream(src);
      FileOutputStream fout = new FileOutputStream(dest);
      int c;
      while ((c = fin.read()) >= 0)
        fout.write(c);
      fin.close();
      fout.close();
    }
  }

}

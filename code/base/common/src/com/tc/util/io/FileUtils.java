/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
    List queue = new LinkedList();
    queue.add(new CopyTask(src.getCanonicalFile(), dest.getCanonicalFile()));

    while (queue.size() > 0) {
      CopyTask item = (CopyTask) queue.remove(0);
      if (item.getSrc().isDirectory()) {
        File destDir = item.getDest();
        destDir.mkdirs();

        if (!destDir.isDirectory()) { throw new IOException("Destination directory does not exist: " + destDir); }

        String[] list = item.getSrc().list();
        for (int i = 0; i < list.length; i++) {
          File _src = new File(item.getSrc(), list[i]);
          File _dest = new File(item.getDest(), list[i]);
          queue.add(new CopyTask(_src, _dest));
        }
      } else if (item.getSrc().isFile()) {
        try {
          doCopy(item.getSrc(), item.getDest());
        } catch (IOException e) {
          System.err.println("Error copying: [" + item.getSrc() + "] to [" + item.getDest() + "]");
        }
      } else {
        throw new IOException(item.getSrc() + " is neither a file or a directory");
      }
    }

  }

  private static void doCopy(File src, File dest) throws IOException {
    FileInputStream in = null;
    FileOutputStream out = null;
    byte[] buffer = new byte[1024 * 8];
    int count;
    try {
      in = new FileInputStream(src);
      out = new FileOutputStream(dest);
      while ((count = in.read(buffer)) >= 0) {
        out.write(buffer, 0, count);
      }
    } finally {
      closeQuietly(in);
      closeQuietly(out);
    }
  }

  private static class CopyTask {
    private final File src;
    private final File dest;

    public CopyTask(File src, File dest) {
      this.src = src;
      this.dest = dest;
    }

    public File getSrc() {
      return src;
    }

    public File getDest() {
      return dest;
    }
  }

  public static void closeQuietly(InputStream is) {
    if (is != null) {
      try {
        is.close();
      } catch (IOException ioe) {
        //
      }
    }
  }

  public static void closeQuietly(OutputStream os) {
    if (os != null) {
      try {
        os.close();
      } catch (IOException ioe) {
        //
      }
    }
  }

}

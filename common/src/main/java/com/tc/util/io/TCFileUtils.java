/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.io;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TCFileUtils {
  
  /**
   * Callback interface for ensureWritableDir(), so that the calling code can report problems to the user.
   */
  public interface EnsureWritableDirReporter {
    /** 
     * called if directory exists but is read-only
     * @param dir the directory that was being checked (will be non-null)
     * @param e an exception encountered during verification, or null if none was encountered.
     */
    void reportReadOnly(File dir, Exception e);
    /** 
     * called if directory does not exist and could not be created, or exists but
     * is not a directory (e.g., an ordinary file).
     * @param dir the directory that was being checked (will be non-null)
     * @param e an exception encountered during dir creation, or null if none was encountered.
     */
    void reportFailedCreate(File dir, Exception e);
  }
  
  /**
   * Ensure that a directory exists and is writable. If it does not exist, try to create it.
   * @param dir must be non-null
   * @param reporter will be called if a problem is encountered. May be null if reporting
   * is not desired
   * @return true if the directory exists and is writable on return.
   */
  public static boolean ensureWritableDir(File dir, EnsureWritableDirReporter reporter) {
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
      // verify that dir exists and is a directory
      if (!dir.isDirectory()) {
        reporter.reportFailedCreate(dir, null);
        return false;
      }
      // check write permissions
      if (!dir.canWrite()) {
        reporter.reportReadOnly(dir, null);
        return false;
      }
    } catch (NullPointerException npe) {
      // rethrow NPE - nulls are a programming error, shouldn't happen
      throw npe;
    } catch (Exception e) {
      reporter.reportFailedCreate(dir, e);
      return false;
    }
    return true;

  }

  /**
   * Given a resource path, returns the File object of that resource
   */
  public static File getResourceFile(String resource) { 
    return org.apache.commons.io.FileUtils.toFile(TCFileUtils.class.getResource(resource));
  }
  
  /**
   * deletes all files with matching extension. Does not recurse into sub directories.
   */
  public static void forceDelete(File directory, String extension) throws IOException {
    Iterator files = org.apache.commons.io.FileUtils.listFiles(directory, new String[] { extension }, false).iterator();
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
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
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
}

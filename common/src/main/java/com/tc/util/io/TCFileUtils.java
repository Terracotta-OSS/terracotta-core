/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.io;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
   * copy one file to another. Can also copy directories
   */
  public static void copyFile(File src, File dest) throws IOException {
    List<CopyTask> queue = new LinkedList<CopyTask>();
    queue.add(new CopyTask(src.getCanonicalFile(), dest.getCanonicalFile()));

    while (queue.size() > 0) {
      CopyTask item = queue.remove(0);
      if (item.getSrc().isDirectory()) {
        File destDir = item.getDest();
        destDir.mkdirs();

        if (!destDir.isDirectory()) { throw new IOException("Destination directory does not exist: " + destDir); }

        String[] list = item.getSrc().list();
        if (list != null) {
          for (String element : list) {
            File _src = new File(item.getSrc(), element);
            File _dest = new File(item.getDest(), element);
            queue.add(new CopyTask(_src, _dest));
          }
        } else {
          throw new RuntimeException("Error listing contents of [" + item.getSrc() + "]");
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

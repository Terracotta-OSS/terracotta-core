/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import java.io.File;
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

}

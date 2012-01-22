/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Translates an array of Class objects to an array of File objects. NOTE: This class only supports a single level of
 * nested inner classes, i.e. no innerclass of an innerclass
 */
public final class ClassListToFileList {

  /**
   * @return [0][] is a list of absolute file paths, [1][] is a list of relative paths starting at the package name
   *         (com)
   */
  public static File[][] translate(Class[] classes) throws FileNotFoundException {
    List files = new ArrayList();
    List relativeFiles = new ArrayList();
    File file;
    File relativeFile;
    File inner;
    int offset;
    for (int i = 0; i < classes.length; i++) {
      relativeFile = new File(classes[i].getName().replaceAll("\\.", "/") + ".class");
      relativeFiles.add(relativeFile);
      String[] parts = classes[i].getName().split("\\.");
      URL url = classes[i].getResource(parts[parts.length - 1] + ".class");
      file = new File(url.getPath());
      files.add(file);
      offset = file.toString().length() - relativeFile.toString().length();
      if (!file.exists()) throw new FileNotFoundException("Unable to resolve class file location for: " + classes[i]);
      String[] packageFiles = file.getParentFile().list();
      for (int j = 0; j < packageFiles.length; j++) {
        if (Pattern.matches(file.getName().replaceFirst("\\.class", "") + "(\\$.*)\\.class", packageFiles[j])) {
          inner = new File(file.getParent() + File.separator + packageFiles[j]);
          files.add(inner);
          relativeFiles.add(new File(inner.toString().substring(offset)));
        }
      }
    }
    File[][] retVal = new File[2][];
    retVal[0] = (File[]) files.toArray(new File[0]);
    retVal[1] = (File[]) relativeFiles.toArray(new File[0]);
    return retVal;
  }
}

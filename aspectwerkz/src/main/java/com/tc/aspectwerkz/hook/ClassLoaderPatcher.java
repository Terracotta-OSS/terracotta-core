/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.hook;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods to manipulate class redefinition of java.lang.ClassLoader in xxxStarter
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class ClassLoaderPatcher {
  /**
   * Converts an input stream to a byte[]
   */
  public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    for (int b = is.read(); b != -1; b = is.read()) {
      os.write(b);
    }
    return os.toByteArray();
  }

  /**
   * Gets the bytecode of the modified java.lang.ClassLoader using given ClassLoaderPreProcessor class name
   */
  static byte[] getPatchedClassLoader(String preProcessorName) {
    byte[] abyte = null;
    InputStream is = null;
    try {
      is = ClassLoader.getSystemClassLoader().getParent().getResourceAsStream("java/lang/ClassLoader.class");
      abyte = inputStreamToByteArray(is);
    } catch (IOException e) {
      throw new Error("failed to read java.lang.ClassLoader: " + e.toString());
    } finally {
      try {
        is.close();
      } catch (Exception e) {
        ;
      }
    }
    if (preProcessorName != null) {
      try {
        ClassLoaderPreProcessor clpi = (ClassLoaderPreProcessor) Class.forName(preProcessorName).newInstance();
        abyte = clpi.preProcess(abyte);
      } catch (Exception e) {
        System.err.println("failed to instrument java.lang.ClassLoader: preprocessor not found");
        e.printStackTrace();
      }
    }
    return abyte;
  }

  /**
   * Dump bytecode bytes in dir/className.class directory, created if needed
   */
  private static void writeClass(String className, byte[] bytes, String dir) {
    String filename = dir + File.separatorChar + className.replace('.', File.separatorChar) + ".class";
    int pos = filename.lastIndexOf(File.separatorChar);
    if (pos > 0) {
      String finalDir = filename.substring(0, pos);
      (new File(finalDir)).mkdirs();
    }
    try {
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
      out.write(bytes);
      out.close();
    } catch (IOException e) {
      System.err.println("failed to write " + className + " in " + dir);
      e.printStackTrace();
    }
  }

  /**
   * Patch java.lang.ClassLoader with preProcessorName instance and dump class bytecode in dir
   */
  public static void patchClassLoader(String preProcessorName, String dir) {
    byte[] cl = getPatchedClassLoader(preProcessorName);
    writeClass("java.lang.ClassLoader", cl, dir);
  }

}

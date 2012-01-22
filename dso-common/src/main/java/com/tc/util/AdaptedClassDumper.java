/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

/**
 * A little utility class that will write class files to disk for instrumented class files.
 */
public class AdaptedClassDumper extends AbstractClassDumper {
  
  public final static AdaptedClassDumper INSTANCE = new AdaptedClassDumper();
  
  private AdaptedClassDumper()  {
    // make the default constructor private to turn this class into a singleton
  }
  
  protected String getDumpDirectoryName() {
    return "adapted";
  }

  protected String getPropertyName() {
    return "tc.classloader.writeToDisk";
  }
}

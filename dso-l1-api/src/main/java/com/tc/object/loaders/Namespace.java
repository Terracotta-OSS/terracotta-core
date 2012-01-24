/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Manage classloader namespaces
 */
public class Namespace {

  private static final String LOGICAL_CLASS_EXTENDS_SEPARATOR = "::";

  /**
   * @return Separator in logical class extension
   */
  public static String getLogicalClassExtendsSeparator() {
    return LOGICAL_CLASS_EXTENDS_SEPARATOR;
  }

  /**
   * Create logical extending class name by combining class names
   * 
   * @param className Class name
   * @param superClassName Logical super class name
   */
  public static String createLogicalExtendingClassName(String className, String superClassName) {
    return className + LOGICAL_CLASS_EXTENDS_SEPARATOR + superClassName;
  }

  /**
   * Parse class name out of logical extending class name
   * 
   * @param className Logical extending name, as returned by {@link #createLogicalExtendingClassName(String, String)}
   * @return Extending class name
   */
  public static String parseClassNameIfNecessary(String className) {
    int separatorIndex = className.indexOf(LOGICAL_CLASS_EXTENDS_SEPARATOR);
    if (separatorIndex == -1) { return className; }
    return className.substring(0, separatorIndex);
  }

  /**
   * Parse super class name out of logical extending class name
   * 
   * @param className Logical extending name, as returned by {@link #createLogicalExtendingClassName(String, String)}
   * @return Logical super class name
   */
  public static String parseLogicalNameIfNeceesary(String className) {
    int separatorIndex = className.indexOf(LOGICAL_CLASS_EXTENDS_SEPARATOR);
    if (separatorIndex == -1) { return null; }
    return className.substring(separatorIndex + LOGICAL_CLASS_EXTENDS_SEPARATOR.length());
  }

  private Namespace() {
    //
  }
}

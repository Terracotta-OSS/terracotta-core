/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Manage classloader namespaces
 */
public class Namespace {

  // The separator is . coz classes are generated in the server with these as a part of the package name
  private static final String SEP                             = ".";

  // this should never be found in className or loaderDescription
  private static final String CLASS_NAME_LOADER_SEPARATOR     = ":://::";

  private static final String LOGICAL_CLASS_EXTENDS_SEPARATOR = "::";

  // top level loader namespaces
  public static final String  STANDARD_NAMESPACE              = "Standard" + SEP;
  public static final String  TOMCAT_NAMESPACE                = "Tomcat" + SEP;
  public static final String  GERONIMO_NAMESPACE              = "Geronimo" + SEP;
  public static final String  WEBLOGIC_NAMESPACE              = "Weblogic" + SEP;
  public static final String  JBOSS_NAMESPACE                 = "JBoss" + SEP;
  public static final String  MODULES_NAMESPACE               = "Modules" + SEP;
  public static final String  JETTY_NAMESPACE                 = "Jetty" + SEP;
  public static final String  GLASSFISH_NAMESPACE             = "Glassfish" + SEP;

  // Well-known loader names.  These names may be hard-coded into test cases: take care if you change them.
  private static final String SYSTEM_LOADER_NAME              = STANDARD_NAMESPACE + "system";
  private static final String EXT_LOADER_NAME                 = STANDARD_NAMESPACE + "ext";
  private static final String BOOT_LOADER_NAME                = STANDARD_NAMESPACE + "bootstrap";
  private static final String ISOLATION_LOADER_NAME           = "com.tc.object.loaders.IsolationClassLoader";

  /**
   * @return Normal system class loader name
   */
  public static String getStandardSystemLoaderName() {
    return SYSTEM_LOADER_NAME;
  }

  /**
   * @return Extensions class loader name
   */
  public static String getStandardExtensionsLoaderName() {
    return EXT_LOADER_NAME;
  }

  /**
   * @return Boot class loader name
   */
  public static String getStandardBootstrapLoaderName() {
    return BOOT_LOADER_NAME;
  }

  /**
   * @return Isolation class loader name - used only for testing
   */
  public static String getIsolationLoaderName() {
    return ISOLATION_LOADER_NAME;
  }
  
  /**
   * @return Separator between loader and class name
   */
  public static String getClassNameAndLoaderSeparator() {
    return CLASS_NAME_LOADER_SEPARATOR;
  }

  /**
   * @return Separator in logical class extension
   */
  public static String getLogicalClassExtendsSeparator() {
    return LOGICAL_CLASS_EXTENDS_SEPARATOR;
  }

  /**
   * Create logical extending class name by combining class names
   * @param className Class name
   * @param superClassName Logical super class name
   */
  public static String createLogicalExtendingClassName(String className, String superClassName) {
    return className + LOGICAL_CLASS_EXTENDS_SEPARATOR + superClassName;
  }

  /**
   * Parse class name out of logical extending class name
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
   * @param className Logical extending name, as returned by {@link #createLogicalExtendingClassName(String, String)}
   * @return Logical super class name
   */
  public static String parseLogicalNameIfNeceesary(String className) {
    int separatorIndex = className.indexOf(LOGICAL_CLASS_EXTENDS_SEPARATOR);
    if (separatorIndex == -1) { return null; }
    return className.substring(separatorIndex + LOGICAL_CLASS_EXTENDS_SEPARATOR.length());
  }

  /**
   * Create a loader name based on a toplevel loader name and a subname
   * @param topLevel Top level name
   * @param subName Sub level name
   * @return Classloader name
   */
  public static String createLoaderName(String topLevel, String subName) {
    if (topLevel == null) { throw new IllegalArgumentException("topLevel space is null"); }
    if (subName == null) { throw new IllegalArgumentException("subName is null"); }

    if (topLevel.equals(TOMCAT_NAMESPACE) || topLevel.equals(WEBLOGIC_NAMESPACE) || topLevel.equals(GERONIMO_NAMESPACE)
        || topLevel.equals(JBOSS_NAMESPACE) || topLevel.equals(MODULES_NAMESPACE) || topLevel.equals(JETTY_NAMESPACE)
        || topLevel.equals(GLASSFISH_NAMESPACE)) {
      // this check will probably need to evolve over time, it's obviously not fancy enough yet
      return new StringBuffer(topLevel).append(subName).toString();
    }

    throw new IllegalArgumentException("Invalid top level namespace: " + topLevel);
  }

  private Namespace() {
    //
  }
}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

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

  private static final String SYSTEM_LOADER_NAME              = STANDARD_NAMESPACE + "system";
  private static final String EXT_LOADER_NAME                 = STANDARD_NAMESPACE + "ext";
  private static final String BOOT_LOADER_NAME                = STANDARD_NAMESPACE + "bootstrap";

  public static String getStandardSystemLoaderName() {
    return SYSTEM_LOADER_NAME;
  }

  public static String getStandardExtensionsLoaderName() {
    return EXT_LOADER_NAME;
  }

  public static String getStandardBootstrapLoaderName() {
    return BOOT_LOADER_NAME;
  }

  public static String getClassNameAndLoaderSeparator() {
    return CLASS_NAME_LOADER_SEPARATOR;
  }

  public static String getLogicalClassExtendsSeparator() {
    return LOGICAL_CLASS_EXTENDS_SEPARATOR;
  }

  public static String createLogicalExtendingClassName(String className, String superClassName) {
    return className + LOGICAL_CLASS_EXTENDS_SEPARATOR + superClassName;
  }

  public static String parseClassNameIfNecessary(String className) {
    int separatorIndex = className.indexOf(LOGICAL_CLASS_EXTENDS_SEPARATOR);
    if (separatorIndex == -1) { return className; }
    return className.substring(0, separatorIndex);
  }

  public static String parseLogicalNameIfNeceesary(String className) {
    int separatorIndex = className.indexOf(LOGICAL_CLASS_EXTENDS_SEPARATOR);
    if (separatorIndex == -1) { return null; }
    return className.substring(separatorIndex + LOGICAL_CLASS_EXTENDS_SEPARATOR.length());
  }

  public static String createLoaderName(String topLevel, String subName) {
    if (topLevel == null) { throw new IllegalArgumentException("topLevel space is null"); }
    if (subName == null) { throw new IllegalArgumentException("subName is null"); }

    if (topLevel.equals(TOMCAT_NAMESPACE) || topLevel.equals(WEBLOGIC_NAMESPACE) || topLevel.equals(GERONIMO_NAMESPACE)
        || topLevel.equals(JBOSS_NAMESPACE)) {
      // this check will probably need to evolve over time, it's obviously not fancy enough yet
      return new StringBuffer(topLevel).append(subName).toString();
    }

    throw new IllegalArgumentException("Invalid top level namespace: " + topLevel);
  }

  private Namespace() {
    //
  }
}

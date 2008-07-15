/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.bundles.BundleSpec;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Terracotta Integration Module Util This should be the only source where the TIM names and versions are defined. Check
 * content of integration-modules.properties
 */
public class TIMUtil {
  public static final String      COMMONS_COLLECTIONS_3_1;
  public static final String      GLASSFISH_2_0;
  public static final String      SUREFIRE_2_3;
  public static final String      WEBSPHERE_6_1_0_7;
  public static final String      MODULES_COMMON;

  private static final Properties modules = new Properties();

  static {
    try {
      modules.load(TIMUtil.class.getResourceAsStream("integration-modules.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    COMMONS_COLLECTIONS_3_1 = lookup(".*commons-collections-3.1");
    GLASSFISH_2_0 = lookup(".*glassfish-2.0");
    SUREFIRE_2_3 = lookup(".*surefire-2.3");
    WEBSPHERE_6_1_0_7 = lookup(".*websphere-6.1.0.7");
    MODULES_COMMON = lookup("modules-common");
  }

  private TIMUtil() {
    // singleton
  }

  private static String lookup(String pattern) {
    String name = searchModuleName(pattern);
    if (name == null) { throw new RuntimeException("Can't find module with pattern: [" + pattern + "]"); }
    return name;
  }

  /**
   * @param pattern: java regular expression
   */
  public static String searchModuleName(String pattern) {
    if (modules.containsKey(pattern)) { return pattern; }
    String name = null;
    for (Iterator it = modules.keySet().iterator(); it.hasNext();) {
      String moduleName = (String) it.next();
      if (moduleName.matches(pattern)) {
        name = moduleName;
        break;
      }
    }
    return name;
  }

  public static String getVersion(String moduleName) {
    String spec = modules.getProperty(moduleName);
    BundleSpec bundleSpec = BundleSpec.newInstance(spec);
    return bundleSpec.getVersion();
  }

  public static String getGroupId(String moduleName) {
    String spec = modules.getProperty(moduleName);
    BundleSpec bundleSpec = BundleSpec.newInstance(spec);
    return bundleSpec.getGroupId();
  }

  public static BundleSpec getBundleSpec(String moduleName) {
    String spec = modules.getProperty(moduleName);
    return BundleSpec.newInstance(spec);
  }

  public static Set getModuleNames() {
    return modules.keySet();
  }
}

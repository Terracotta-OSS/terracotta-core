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
  public static final String      APACHE_STRUTS_1_1;
  public static final String      CGLIB_2_1_3;
  public static final String      COMMONS_COLLECTIONS_3_1;
  public static final String      EHCACHE_1_2_4;
  public static final String      EHCACHE_1_3;
  public static final String      GLASSFISH_1_0;
  public static final String      HIBERNATE_3_1_2;
  public static final String      HIBERNATE_3_2_5;
  public static final String      IBATIS_2_2_0;
  public static final String      LUCENE_2_0_0;
  public static final String      RIFE_1_6_0;
  public static final String      SUREFIRE_2_3;
  public static final String      WEBSPHERE_6_1_0_7;
  public static final String      WICKET_1_3;
  public static final String      MODULES_COMMON;
  public static final String      JETTY_6_1;

  private static final Properties modules = new Properties();

  static {
    try {
      modules.load(TIMUtil.class.getResourceAsStream("integration-modules.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    APACHE_STRUTS_1_1 = lookup(".*apache-struts-1.1");
    CGLIB_2_1_3 = lookup(".*cglib-2.1.3");
    COMMONS_COLLECTIONS_3_1 = lookup(".*commons-collections-3.1");
    EHCACHE_1_2_4 = lookup(".*ehcache-1.2.4");
    EHCACHE_1_3 = lookup(".*ehcache-1.3");
    GLASSFISH_1_0 = lookup(".*glassfish-1.0");
    HIBERNATE_3_1_2 = lookup(".*hibernate-3.1.2");
    HIBERNATE_3_2_5 = lookup(".*hibernate-3.2.5");
    IBATIS_2_2_0 = lookup(".*iBatis-2.2.0");
    LUCENE_2_0_0 = lookup(".*lucene-2.0.0");
    RIFE_1_6_0 = lookup(".*rife-1.6.0");
    SUREFIRE_2_3 = lookup(".*surefire-2.3");
    WEBSPHERE_6_1_0_7 = lookup(".*websphere-6.1.0.7");
    WICKET_1_3 = lookup(".*wicket-1.3");
    MODULES_COMMON = lookup("modules-common");
    JETTY_6_1 = lookup(".*jetty-6.1.4");
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

  public static void main(String[] args) {
    System.out.println("Jetty group id: " + TIMUtil.getGroupId(JETTY_6_1));
    System.out.println("Jetty version: " + TIMUtil.getVersion(JETTY_6_1));

    System.out.println("modules-common group id: " + TIMUtil.getGroupId(MODULES_COMMON));
    System.out.println("modules-common version: " + TIMUtil.getVersion(MODULES_COMMON));
  }
}

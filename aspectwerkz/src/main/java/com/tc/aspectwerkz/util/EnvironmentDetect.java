/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.util;

/**
 * Detects Java JVM vendor and Java version
 * Usage: -jvm | -java
 * System.exit code is:
 * 2:BEA, 1:IBM, 0:SUN
 * MajorMinor (f.e. 15) for Java Major.Minor version or 0
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public class EnvironmentDetect {

  public static void main(String a[]) {
    if (a.length < 1) {
      usage();
      show();
      System.exit(-1);
    }
    if (a[0].equals("-jvm")) {
      String vendor = detectJVM();
      if (vendor.indexOf("BEA") >= 0) {
        System.exit(2);
      } else if (vendor.indexOf("IBM") >= 0) {
        System.exit(1);
      } else {
        System.exit(0);
      }
    }
    if (a[0].equals("-java")) {
      String java = detectJava();
      if (java.indexOf("1.5") >= 0) {
        System.exit(15);
      } else if (java.indexOf("1.4") >= 0) {
        System.exit(14);
      } else if (java.indexOf("1.3") >= 0) {
        System.exit(13);
      } else {
        System.exit(0);
      }
    }
    if (a.length > 1) {
      show();
    }
  }

  public static String detectJVM() {
    return System.getProperty("java.vendor").toUpperCase();
  }

  public static String detectJava() {
    return System.getProperty("java.version").toUpperCase();
  }

  public static void show() {
    System.out.println(detectJVM());
    System.out.println(detectJava());
  }

  public static void usage() {
    System.out.println("Usage: -jvm | -java");
  }
}

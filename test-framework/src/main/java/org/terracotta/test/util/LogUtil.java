/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.test.util;

/**
 * Just a util classes for delegating all the logs to single place. Easier to replace with any good logging framework
 * going futher
 */
public class LogUtil {


  public static void info(Class<?> clazz, String message) {
    System.out.println(clazz.getSimpleName() + " :: " + message);
  }

  public static void debug(Class<?> clazz, String message) {
    System.out.println(clazz.getSimpleName() + " :: " + message);
  }
}

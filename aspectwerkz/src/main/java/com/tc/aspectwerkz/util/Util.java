/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.util;

import com.tc.aspectwerkz.reflect.ReflectionInfo;

/**
 * Utility methods and constants used in the AspectWerkz system.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class Util {
  public static final Integer INTEGER_DEFAULT_VALUE = Integer.valueOf(0);

  public static final Float FLOAT_DEFAULT_VALUE = new Float(0.0f);

  public static final Double DOUBLE_DEFAULT_VALUE = new Double(0.0d);

  public static final Long LONG_DEFAULT_VALUE = Long.valueOf(0L);

  public static final Boolean BOOLEAN_DEFAULT_VALUE = new Boolean(false);

  public static final Character CHARACTER_DEFAULT_VALUE = new Character('\u0000');

  public static final Short SHORT_DEFAULT_VALUE;

  public static final Byte BYTE_DEFAULT_VALUE;

  static {
    byte b = 0;
    BYTE_DEFAULT_VALUE = new Byte(b);
    short s = 0;
    SHORT_DEFAULT_VALUE = new Short(s);
  }

  /**
   * Calculates the hash for the class name and the meta-data.
   *
   * @param className the class name
   * @param info      the meta-data
   * @return the hash
   */
  public static Integer calculateHash(final String className, final ReflectionInfo info) {
    if (className == null) {
      throw new IllegalArgumentException("class name can not be null");
    }
    if (info == null) {
      throw new IllegalArgumentException("info can not be null");
    }
    int hash = 17;
    hash = (37 * hash) + className.hashCode();
    hash = (37 * hash) + info.hashCode();
    Integer hashKey = Integer.valueOf(hash);
    return hashKey;
  }

  /**
   * Removes the AspectWerkz specific elements from the stack trace. <p/>TODO: how to mess w/ the stacktrace in JDK
   * 1.3.x?
   *
   * @param exception the Throwable to modify the stack trace on
   * @param className the name of the fake origin class of the exception
   */
  public static void fakeStackTrace(final Throwable exception, final String className) {
    if (exception == null) {
      throw new IllegalArgumentException("exception can not be null");
    }
    if (className == null) {
      throw new IllegalArgumentException("class name can not be null");
    }

    //        final List newStackTraceList = new ArrayList();
    //        final StackTraceElement[] stackTrace = exception.getStackTrace();
    //        int i;
    //        for (i = 1; i < stackTrace.length; i++) {
    //            if (stackTrace[i].getClassName().equals(className)) break;
    //        }
    //        for (int j = i; j < stackTrace.length; j++) {
    //            newStackTraceList.add(stackTrace[j]);
    //        }
    //
    //        final StackTraceElement[] newStackTrace =
    //                new StackTraceElement[newStackTraceList.size()];
    //        int k = 0;
    //        for (Iterator it = newStackTraceList.iterator(); it.hasNext(); k++) {
    //            final StackTraceElement element = (StackTraceElement)it.next();
    //            newStackTrace[k] = element;
    //        }
    //        exception.setStackTrace(newStackTrace);
  }

  /**
   * Returns a String representation of a classloader Avoid to do a toString() if the resulting string is too long
   * (occurs on Tomcat)
   *
   * @param loader
   * @return String representation (toString or FQN@hashcode)
   */
  public static String classLoaderToString(ClassLoader loader) {
    if ((loader != null) && (loader.toString().length() < 120)) {
      return loader.toString() + "@" + loader.hashCode();
    } else if (loader != null) {
      return loader.getClass().getName() + "@" + loader.hashCode();
    } else {
      return "null";
    }
  }

  /**
   * Helper method to support Java 1.4 like Boolean.valueOf(boolean) in Java 1.3
   *
   * @param b
   * @return
   */
  public static Boolean booleanValueOf(boolean b) {
    return b ? Boolean.TRUE : Boolean.FALSE;
  }
}

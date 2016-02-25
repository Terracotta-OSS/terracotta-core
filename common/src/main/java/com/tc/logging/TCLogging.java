/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.logging;

import java.io.File;
import java.util.Arrays;

/**
 * Factory class for obtaining TCLogger instances.
 * 
 * @author teck
 */
public class TCLogging {

  public static final String        LOG_CONFIGURATION_PREFIX           = "The configuration read for Logging: ";

  static final String[]     INTERNAL_LOGGER_NAMESPACES         = new String[] { "com.tc", "com.terracotta",
      "com.terracottatech", "org.terracotta", "tc.operator"           };

  static final String       CUSTOMER_LOGGER_NAMESPACE          = "com.terracottatech";
  static final String       CUSTOMER_LOGGER_NAMESPACE_WITH_DOT = CUSTOMER_LOGGER_NAMESPACE + ".";

  static final String       CONSOLE_LOGGER_NAME                = CUSTOMER_LOGGER_NAMESPACE + ".console";
  public static final String        DUMP_LOGGER_NAME                   = "com.tc.dumper.dump";
  static final String       OPERATOR_EVENT_LOGGER_NAME         = "tc.operator.event";

  public static final String        LOG4J_PROPERTIES_FILENAME          = ".tc.dev.log4j.properties";

  public static final String        DUMP_PATTERN                       = "[dump] %m%n";
  public static final String        DERBY_PATTERN                      = "[derby.log] %m%n";

  public static final String        FILE_AND_JMX_PATTERN               = "%d [%t] %p %c - %m%n";

  private static  DelegatingTCLogger DELEGATE;

  static {
    try {
      Thread.currentThread().getContextClassLoader().loadClass("org.apache.log4j.varia.NullAppender");
      DELEGATE = Log4jTCLogging.getDelegate();
    } catch (ClassNotFoundException e) {
      DELEGATE = Slf4jTCLogging.getDelegate();
    }
  }

  public static TCLogger getLogger(Class<?> clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getLogger(clazz.getName());
  }

  public static TCLogger getLogger(String name) {
    if (name == null) { throw new NullPointerException("Logger cannot be null"); }

    boolean allowedName = false;
    for (String namespace : INTERNAL_LOGGER_NAMESPACES) {
      String withDot = namespace + ".";
      if (name.startsWith(withDot)) {
        allowedName = true;
        break;
      }
    }

    if (!allowedName) {
      //
      throw new IllegalArgumentException("Logger name (" + name + ") not in valid namespace: "
                                         + Arrays.asList(INTERNAL_LOGGER_NAMESPACES));
    }

    return DELEGATE.newLogger(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("Name cannot be null"); }
    return DELEGATE.newLogger(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(Class<?> clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getTestingLogger(clazz.getName());
  }

  // You want to look at CustomerLogging to get customer facing logger instances
  static TCLogger getCustomerLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("name cannot be null"); }

    name = CUSTOMER_LOGGER_NAMESPACE_WITH_DOT + name;

    if (CONSOLE_LOGGER_NAME.equals(name)) { throw new IllegalArgumentException("Illegal name: " + name); }

    return DELEGATE.newLogger(name);
  }

  // this method not public on purpose, use CustomerLogging.getConsoleLogger() instead
  static TCLogger getConsoleLogger() {
    return DELEGATE.getConsoleLogger();
  }

  static TCLogger getOperatorEventLogger() {
    return DELEGATE.getOperatorEventLogger();
  }

  /**
   * <strong>FOR TESTS ONLY</strong>. This allows tests to successfully blow away directories containing log files on
   * Windows. This is a bit of a hack, but working around it is otherwise an enormous pain &mdash; tests only fail on
   * Windows, and you must then very carefully go around, figure out exactly why, and then work around it. Use of this
   * method makes everything a great deal simpler.
   */
  public static synchronized void disableLocking() {
    DELEGATE.disableLocking();
  }

  public static final int PROCESS_TYPE_GENERIC = 0;
  public static final int PROCESS_TYPE_L1      = 1;
  public static final int PROCESS_TYPE_L2      = 2;

  @SuppressWarnings("resource")
  public static void setLogDirectory(File theDirectory, int processType) {
    DELEGATE.setLogDirectory(theDirectory, processType);
  }

  public static TCLogger getDumpLogger() {
    return DELEGATE.newLogger(DUMP_LOGGER_NAME);
  }

  // This method for use in tests only
  public static void closeFileAppender() {
    DELEGATE.closeFileAppender();
  }

}

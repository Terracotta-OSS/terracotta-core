/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class DefaultLoggerProvider implements TCLoggerProvider {

  public TCLogger getLogger(Class clazz) {
    return TCLogging.getLogger(clazz);
  }

  public TCLogger getLogger(String name) {
    return TCLogging.getLogger(name);
  }

}

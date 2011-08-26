/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

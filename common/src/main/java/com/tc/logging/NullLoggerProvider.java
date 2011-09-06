/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

public class NullLoggerProvider implements TCLoggerProvider {

  public TCLogger getLogger(Class clazz) {
    return new NullTCLogger();
  }

  public TCLogger getLogger(String name) {
    return new NullTCLogger();
  }

}

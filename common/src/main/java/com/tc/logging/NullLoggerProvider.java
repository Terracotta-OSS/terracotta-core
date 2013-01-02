/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class NullLoggerProvider implements TCLoggerProvider {

  @Override
  public TCLogger getLogger(Class clazz) {
    return new NullTCLogger();
  }

  @Override
  public TCLogger getLogger(String name) {
    return new NullTCLogger();
  }

}

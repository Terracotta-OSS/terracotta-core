/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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

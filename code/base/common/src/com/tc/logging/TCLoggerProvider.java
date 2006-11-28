/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.logging;

public interface TCLoggerProvider {
  public TCLogger getLogger(Class clazz);
  public TCLogger getLogger(String name);
}

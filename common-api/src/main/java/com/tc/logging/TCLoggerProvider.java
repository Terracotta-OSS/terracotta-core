/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public interface TCLoggerProvider {
  public TCLogger getLogger(Class clazz);
  public TCLogger getLogger(String name);
}

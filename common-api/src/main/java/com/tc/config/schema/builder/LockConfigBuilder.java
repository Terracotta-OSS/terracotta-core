/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.builder;

public interface LockConfigBuilder {

  public static final String TAG_AUTO_LOCK  = "autolock";
  public static final String TAG_NAMED_LOCK = "named-lock";

  public void setLockName(String value);

  public void setMethodExpression(String value);

  public static final String LEVEL_WRITE             = "write";
  public static final String LEVEL_READ              = "read";
  public static final String LEVEL_CONCURRENT        = "concurrent";
  public static final String LEVEL_SYNCHRONOUS_WRITE = "synchronous-write";

  public void setLockLevel(String value);

  public void setLockName(int value);

}
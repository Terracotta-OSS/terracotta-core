/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

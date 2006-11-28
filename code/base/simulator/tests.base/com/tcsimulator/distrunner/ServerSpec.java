/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tcsimulator.distrunner;

import java.util.List;

public interface ServerSpec {
  public static final int CONTROL_SERVER = 0;
  public static final int TEST_SERVER    = 1;
  public static final int BACKUP_SERVER  = 2;

  public boolean isNull();

  public String getHostName();

  public String getTestHome();

  public List getJvmOpts();

  public int getCache();

  public int getJmxPort();

  public int getDsoPort();

  public ServerSpec copy();

  public int getType();
}

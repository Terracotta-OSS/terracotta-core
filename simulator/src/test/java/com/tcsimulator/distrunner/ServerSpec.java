/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

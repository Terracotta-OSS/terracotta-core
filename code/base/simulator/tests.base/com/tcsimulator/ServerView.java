/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import java.util.List;

public interface ServerView {
  public static final int RUNNING     = 0;
  public static final int NOT_RUNNING = 1;

  public String getHostName();

  public String getTestHome();

  public List getJvmOpts();

  public int getJmxPort();

  public int getDsoPort();

  public int getCacheCount();

  public int getType();

  public ServerView copy();

  // public int getClientCount();

  public int isServerRunning();

}

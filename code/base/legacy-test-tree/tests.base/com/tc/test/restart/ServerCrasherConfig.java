/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.restart;

import com.tc.objectserver.control.ServerControl;

public interface ServerCrasherConfig {

  public ServerControl getServerControl();

  public boolean isCrashy();
  public void setIsCrashy(boolean b);

  public long getRestartInterval();
  public void setRestartInterval(long milliseconds);

}

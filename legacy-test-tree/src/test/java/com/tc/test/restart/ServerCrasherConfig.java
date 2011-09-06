/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

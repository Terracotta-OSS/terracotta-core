/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;

import java.io.Serializable;

/**
 * A portion of a deadlock chain
 */
public interface DeadlockChain extends Serializable {
  public ClientID getWaiterClient();

  public ThreadID getWaiterThread();
  
  public DeadlockChain getNextLink();

  public LockID getWaitingOn();
}

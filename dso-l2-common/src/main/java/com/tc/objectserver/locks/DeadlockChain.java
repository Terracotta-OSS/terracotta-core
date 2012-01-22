/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

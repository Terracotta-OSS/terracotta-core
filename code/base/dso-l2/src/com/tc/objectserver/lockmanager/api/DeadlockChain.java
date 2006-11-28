/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ServerThreadID;

import java.io.Serializable;

/**
 * A portion of a deadlock chain
 */
public interface DeadlockChain extends Serializable {
  public ServerThreadID getWaiter();

  public DeadlockChain getNextLink();

  public LockID getWaitingOn();
}

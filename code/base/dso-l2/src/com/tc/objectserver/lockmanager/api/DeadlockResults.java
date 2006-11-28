/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

/**
 * A callback interface for someone interested in the results of a deadlock detection run
 */
public interface DeadlockResults {

  public void foundDeadlock(DeadlockChain chain);

}

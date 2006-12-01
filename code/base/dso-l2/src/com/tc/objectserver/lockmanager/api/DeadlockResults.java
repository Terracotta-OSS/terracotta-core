/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

/**
 * A callback interface for someone interested in the results of a deadlock detection run
 */
public interface DeadlockResults {

  public void foundDeadlock(DeadlockChain chain);

}

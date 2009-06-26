/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.lockmanager.impl.TimerKey;

public interface LockWaitContext extends TimerKey {

  public NodeID getNodeID();

  public ThreadID getThreadID();

  public TimerSpec getTimerSpec();

  public long getTimestamp();

  public int lockLevel();

  public Sink getLockResponseSink();

  public void waitTimeout();

}

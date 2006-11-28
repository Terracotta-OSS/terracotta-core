/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.WaitInvocation;

public interface LockWaitContext {

  public ChannelID getChannelID();

  public ThreadID getThreadID();

  public WaitInvocation getWaitInvocation();

  public long getTimestamp();

  public boolean wasUpgrade();
  
  public int lockLevel();

  public Sink getLockResponseSink();

  public void waitTimeout();

}

/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.msg;

public interface ClientHandshakeAckMessage {
  public void send();

  public long getObjectIDSequenceStart();

  public long getObjectIDSequenceEnd();

  public boolean getPersistentServer();

  public void initialize(long start, long end, boolean persistent);
}

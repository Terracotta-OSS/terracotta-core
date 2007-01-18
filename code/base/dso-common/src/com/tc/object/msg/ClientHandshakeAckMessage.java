/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.MessageChannel;

public interface ClientHandshakeAckMessage {
  public void send();

  public long getObjectIDSequenceStart();

  public long getObjectIDSequenceEnd();

  public boolean getPersistentServer();

  public void initialize(long start, long end, boolean persistent);

  public MessageChannel getChannel();
}

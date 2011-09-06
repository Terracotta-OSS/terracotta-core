/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.session.SessionID;

public class TestLockRequestMessageFactory implements LockRequestMessageFactory {

  public LockRequestMessage newLockRequestMessage(final NodeID nodeId) {
    TestMessageChannel channel = new TestMessageChannel();
    channel.channelID = new ChannelID(100);
    return new LockRequestMessage(new SessionID(100), new NullMessageMonitor(), new TCByteBufferOutputStream(),
                                  channel, TCMessageType.LOCK_REQUEST_MESSAGE);
  }

}

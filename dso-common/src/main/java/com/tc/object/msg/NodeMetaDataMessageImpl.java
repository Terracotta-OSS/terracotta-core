/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

public class NodeMetaDataMessageImpl extends DSOMessageBase implements NodeMetaDataMessage {

  private static final byte THREAD_ID = 1;
  private static final byte NODE_ID   = 2;

  private ThreadID          threadID;

  private NodeID            nodeID;

  public NodeMetaDataMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                 final TCByteBufferOutputStream out, final MessageChannel channel,
                                 final TCMessageType messageType) {
    super(sessionID, monitor, out, channel, messageType);
  }

  public NodeMetaDataMessageImpl(final SessionID sessionID, final MessageMonitor monitor, final MessageChannel channel,
                                 final TCMessageHeader header, final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public void setThreadID(final ThreadID threadID) {
    this.threadID = threadID;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public void setNodeID(final NodeID nodeID) {
    this.nodeID = nodeID;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertNotNull(nodeID);

    putNVPair(THREAD_ID, threadID.toLong());
    putNVPair(NODE_ID, nodeID);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case NODE_ID:
        nodeID = getNodeIDValue();
        return true;
      default:
        return false;
    }
  }
}

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
import com.tc.object.ObjectID;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodesWithObjectsResponseMessageImpl extends DSOMessageBase implements NodesWithObjectsResponseMessage {

  private final static byte          THREAD_ID         = 1;
  private final static byte          MANAGED_OBJECT_ID = 2;
  private final static byte          NODE_ID           = 3;

  private ThreadID                   threadID;
  private Map<ObjectID, Set<NodeID>> nodesWithObjects;

  private Set<NodeID>                lastHydratedNodeIDSet;

  public NodesWithObjectsResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                             final TCByteBufferOutputStream out, final MessageChannel channel,
                                             final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public NodesWithObjectsResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                             final MessageChannel channel, final TCMessageHeader header,
                                             final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(final ThreadID tID, final Map<ObjectID, Set<NodeID>> response) {
    this.threadID = tID;
    this.nodesWithObjects = response;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertNotNull(nodesWithObjects);

    putNVPair(THREAD_ID, threadID.toLong());

    for (Map.Entry<ObjectID, Set<NodeID>> entry : nodesWithObjects.entrySet()) {
      putNVPair(MANAGED_OBJECT_ID, entry.getKey().toLong());

      for (NodeID nodeID : entry.getValue()) {
        putNVPair(NODE_ID, nodeID);
      }
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    if (null == nodesWithObjects) {
      nodesWithObjects = new HashMap<ObjectID, Set<NodeID>>();
    }

    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case MANAGED_OBJECT_ID:
        ObjectID objectID = new ObjectID(getLongValue());
        lastHydratedNodeIDSet = new HashSet<NodeID>();
        nodesWithObjects.put(objectID, lastHydratedNodeIDSet);
        return true;
      case NODE_ID:
        Assert.assertNotNull(lastHydratedNodeIDSet);
        NodeID nodeID = getNodeIDValue();
        lastHydratedNodeIDSet.add(nodeID);
        return true;
      default:
        return false;
    }
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public Map<ObjectID, Set<NodeID>> getNodesWithObjects() {
    return nodesWithObjects;
  }
}
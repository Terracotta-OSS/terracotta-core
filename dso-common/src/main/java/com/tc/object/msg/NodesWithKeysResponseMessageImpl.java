/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Alex Snaps
 */
public class NodesWithKeysResponseMessageImpl extends DSOMessageBase implements NodesWithKeysResponseMessage {

  private static final byte        THREAD_ID    = 0;
  private static final byte        KEY_SET_SIZE = 1;

  private ThreadID                 threadID;
  private Map<Object, Set<NodeID>> response;

  public NodesWithKeysResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                          final TCByteBufferOutputStream out, final MessageChannel channel,
                                          final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public NodesWithKeysResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                          final MessageChannel channel, final TCMessageHeader header,
                                          final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(final ThreadID tid, final Map<Object, Set<NodeID>> resp) {
    this.threadID = tid;
    this.response = resp;
  }

  public Map<Object, Set<NodeID>> getNodesWithKeys() {
    return response;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  @Override
  public void dehydrateValues() {
    putNVPair(THREAD_ID, threadID.toLong());
    putNVPair(KEY_SET_SIZE, response.size());
    for (Map.Entry<Object, Set<NodeID>> entry : response.entrySet()) {
      TCByteBufferOutputStream out = getOutputStream();
      out.writeString(entry.getKey().toString());
      out.writeInt(entry.getValue().size());
      for (NodeID nodeID : entry.getValue()) {
        new NodeIDSerializer(nodeID).serializeTo(out);
      }
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    int size;
    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case KEY_SET_SIZE:
        size = getIntValue();
        response = new HashMap<Object, Set<NodeID>>(size, 1.0f);
        while (size-- > 0) {
          Object key;
          key = getInputStream().readString();
          int setSize = getIntValue();
          HashSet<NodeID> set = new HashSet<NodeID>(setSize, 1.0f);
          response.put(key, set);
          while (setSize-- > 0) {
            set.add(getNodeIDValue());
          }
        }
        return true;
      default:
        return false;
    }
  }
}

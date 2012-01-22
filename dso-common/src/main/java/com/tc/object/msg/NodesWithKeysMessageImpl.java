/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Alex Snaps
 */
public class NodesWithKeysMessageImpl extends DSOMessageBase implements NodesWithKeysMessage {

  private static final byte OBJECT_ID = 0;
  private static final byte KEYS_SIZE = 1;
  private static final byte THREAD_ID = 2;

  private ObjectID          objectID;
  private Set<Object>       keys;
  private ThreadID          threadID;

  public NodesWithKeysMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                  final TCByteBufferOutputStream out, final MessageChannel channel,
                                  final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public NodesWithKeysMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                  final MessageChannel channel, final TCMessageHeader header, final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ObjectID getMapObjectID() {
    return objectID;
  }

  public void setMapObjectID(final ObjectID objectID) {
    this.objectID = objectID;
  }

  public void setKeys(final Set<Object> keys) {
    this.keys = keys;
  }

  public Set<Object> getKeys() {
    return keys;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public void setThreadID(final ThreadID threadID) {
    this.threadID = threadID;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(OBJECT_ID, objectID.toLong());
    putNVPair(KEYS_SIZE, keys.size());
    final TCByteBufferOutputStream outStream = getOutputStream();
    for (final Object key : this.keys) {
      outStream.writeString(key.toString());
    }
    putNVPair(THREAD_ID, threadID.toLong());
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    if (null == keys) {
      keys = new HashSet<Object>();
    }

    switch (name) {
      case OBJECT_ID:
        objectID = new ObjectID(getLongValue());
        return true;
      case KEYS_SIZE:
        int size = getIntValue();
        this.keys = new HashSet<Object>(size, 1f);
        while (size-- > 0) {
          this.keys.add(getInputStream().readString());
        }
        return true;
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      default:
        return false;
    }
  }
}

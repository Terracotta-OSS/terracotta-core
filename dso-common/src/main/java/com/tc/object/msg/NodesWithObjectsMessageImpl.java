/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NodesWithObjectsMessageImpl extends DSOMessageBase implements NodesWithObjectsMessage {

  private static final byte   THREAD_ID         = 1;
  private static final byte   MANAGED_OBJECT_ID = 2;

  private final Set<ObjectID> objectIDs         = new HashSet<ObjectID>();

  private ThreadID            threadID;

  public NodesWithObjectsMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final TCByteBufferOutputStream out, final MessageChannel channel,
                                     final TCMessageType messageType) {
    super(sessionID, monitor, out, channel, messageType);
  }

  public NodesWithObjectsMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final MessageChannel channel, final TCMessageHeader header,
                                     final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public Set<ObjectID> getObjectIDs() {
    return objectIDs;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public void setThreadID(final ThreadID threadID) {
    this.threadID = threadID;
  }

  public void addObjectID(final ObjectID objectID) {
    this.objectIDs.add(objectID);
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);

    putNVPair(THREAD_ID, threadID.toLong());

    for (ObjectID objectID : objectIDs) {
      putNVPair(MANAGED_OBJECT_ID, objectID.toLong());
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case MANAGED_OBJECT_ID:
        objectIDs.add(new ObjectID(getLongValue()));
        return true;
      default:
        return false;
    }
  }
}

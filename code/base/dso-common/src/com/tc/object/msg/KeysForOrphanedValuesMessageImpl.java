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
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

public class KeysForOrphanedValuesMessageImpl extends DSOMessageBase implements KeysForOrphanedValuesMessage {

  private static final byte THREAD_ID         = 1;
  private static final byte MANAGED_OBJECT_ID = 2;

  private ObjectID          mapObjectID;

  private ThreadID          threadID;

  public KeysForOrphanedValuesMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                          final TCByteBufferOutputStream out, final MessageChannel channel,
                                          final TCMessageType messageType) {
    super(sessionID, monitor, out, channel, messageType);
  }

  public KeysForOrphanedValuesMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                          final MessageChannel channel, final TCMessageHeader header,
                                          final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ObjectID getMapObjectID() {
    return mapObjectID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public void setMapObjectID(final ObjectID mapObjectID) {
    this.mapObjectID = mapObjectID;
  }

  public void setThreadID(final ThreadID threadID) {
    this.threadID = threadID;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);

    putNVPair(THREAD_ID, threadID.toLong());
    putNVPair(MANAGED_OBJECT_ID, mapObjectID.toLong());
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case MANAGED_OBJECT_ID:
        mapObjectID = new ObjectID(getLongValue());
        return true;
      default:
        return false;
    }
  }
}

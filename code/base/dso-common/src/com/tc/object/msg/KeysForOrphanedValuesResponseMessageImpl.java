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
import java.util.HashSet;
import java.util.Set;

public class KeysForOrphanedValuesResponseMessageImpl extends DSOMessageBase implements
    KeysForOrphanedValuesResponseMessage {

  private final static byte THREAD_ID         = 1;
  private final static byte MANAGED_OBJECT_ID = 2;

  private ThreadID          threadID;
  private Set<ObjectID>     keys;

  public KeysForOrphanedValuesResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                  final TCByteBufferOutputStream out, final MessageChannel channel,
                                                  final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public KeysForOrphanedValuesResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                  final MessageChannel channel, final TCMessageHeader header,
                                                  final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(final ThreadID tID, final Set<ObjectID> response) {
    this.threadID = tID;
    this.keys = response;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertNotNull(keys);

    putNVPair(THREAD_ID, threadID.toLong());

    for (ObjectID key : keys) {
      putNVPair(MANAGED_OBJECT_ID, key.toLong());
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    if (null == keys) {
      keys = new HashSet<ObjectID>();
    }

    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case MANAGED_OBJECT_ID:
        ObjectID objectID = new ObjectID(getLongValue());
        keys.add(objectID);
        return true;
      default:
        return false;
    }
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public Set<ObjectID> getKeys() {
    return keys;
  }
}
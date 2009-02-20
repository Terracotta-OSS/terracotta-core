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
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

public class KeysForOrphanedValuesResponseMessageImpl extends DSOMessageBase implements
    KeysForOrphanedValuesResponseMessage {

  private final static byte THREAD_ID = 1;
  private final static byte KEYS_DNA_ID      = 2;

  private ThreadID          threadID;
  private byte[]               keys;

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

  public void initialize(final ThreadID tID, final byte[] orphanedKeysDNA) {
    this.threadID = tID;
    this.keys = orphanedKeysDNA;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertNotNull(keys);

    putNVPair(THREAD_ID, threadID.toLong());
    putNVPair(KEYS_DNA_ID, keys);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case KEYS_DNA_ID:
        keys = getBytesArray();
        return true;
      default:
        return false;
    }
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public byte[] getOrphanedKeysDNA() {
    return keys;
  }
}
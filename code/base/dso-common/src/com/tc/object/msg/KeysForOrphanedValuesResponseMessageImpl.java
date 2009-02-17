/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class KeysForOrphanedValuesResponseMessageImpl extends DSOMessageBase implements
    KeysForOrphanedValuesResponseMessage {

  private final static byte THREAD_ID = 1;
  private final static byte KEYS      = 2;

  private ThreadID          threadID;
  private Set               keys;

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

  public void initialize(final ThreadID tID, final Set response) {
    this.threadID = tID;
    this.keys = response;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertNotNull(keys);

    putNVPair(THREAD_ID, threadID.toLong());

    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final TCObjectOutputStream objectOut = new TCObjectOutputStream(bytesOut);
    objectOut.writeInt(keys.size());
    for (Object key : keys) {
      objectOut.writeObject(key);
    }
    objectOut.flush();
    putNVPair(KEYS, bytesOut.toByteArray());
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    if (null == keys) {
      keys = new HashSet<Object>();
    }

    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case KEYS:
        final ByteArrayInputStream bytesIn = new ByteArrayInputStream(getBytesArray());
        final TCObjectInputStream objectIn = new TCObjectInputStream(bytesIn);
        try {
          final int size = objectIn.readInt();
          for (int i = 0; i < size; i++) {
            keys.add(objectIn.readObject());
          }
        } catch (ClassNotFoundException e) {
          final IOException ioex = new IOException();
          ioex.initCause(e);
          throw ioex;
        }
        return true;
      default:
        return false;
    }
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public Set getKeys() {
    return keys;
  }
}
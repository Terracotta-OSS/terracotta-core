/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCDataInput;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class KeysForOrphanedValuesResponseMessageImpl extends DSOMessageBase implements
    KeysForOrphanedValuesResponseMessage {

  private final static byte THREAD_ID         = 1;
  private final static byte KEYS_DNA_ID       = 2;
  private final static byte VALUES_OBJECT_IDS = 3;

  private ThreadID          threadID;
  private byte[]            keys;
  private Set<ObjectID>     valueObjectIDs;

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

  public void initialize(final ThreadID tID, final Set<ObjectID> orphanedValuesObjectIDs) {
    this.threadID = tID;
    this.valueObjectIDs = orphanedValuesObjectIDs;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertTrue("It's not possible to have both the keys of the orphaned values and the orphaned values, "
                      + "it's one or the other and at least one of them is mandatory",
                      (keys != null && null == valueObjectIDs) || (null == keys && valueObjectIDs != null));

    putNVPair(THREAD_ID, threadID.toLong());

    if (keys != null) {
      putNVPair(KEYS_DNA_ID, keys);
    }

    if (valueObjectIDs != null) {
      final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      final TCObjectOutputStream objectOut = new TCObjectOutputStream(bytesOut);
      objectOut.writeInt(valueObjectIDs.size());
      for (ObjectID objectID : valueObjectIDs) {
        objectOut.writeLong(objectID.toLong());
      }
      objectOut.flush();

      putNVPair(VALUES_OBJECT_IDS, bytesOut.toByteArray());
    }
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
      case VALUES_OBJECT_IDS:
        final TCDataInput input = new TCByteBufferInputStream(TCByteBufferFactory.wrap(getBytesArray()));
        final Set<ObjectID> objectIDs = new HashSet<ObjectID>();
        final int size = input.readInt();
        for (int i = 0; i < size; i++) {
          objectIDs.add(new ObjectID(input.readLong()));
        }
        valueObjectIDs = objectIDs;
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

  public Set<ObjectID> getOrphanedValuesObjectIDs() {
    return valueObjectIDs;
  }
}
/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class KeysForOrphanedValuesMessageImpl extends DSOMessageBase implements KeysForOrphanedValuesMessage {

  private static final byte    THREAD_ID         = 1;
  private static final byte    MAP_OBJECT_ID     = 2;
  private static final byte    VALUES_OBJECT_IDS = 3;

  private ThreadID             threadID;
  private ObjectID             mapObjectID;
  private Collection<ObjectID> valueObjectIDs;

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

  public Collection<ObjectID> getMapValueObjectIDs() {
    return valueObjectIDs;
  }

  public void setMapObjectID(final ObjectID mapObjectID) {
    this.mapObjectID = mapObjectID;
  }

  public void setThreadID(final ThreadID threadID) {
    this.threadID = threadID;
  }

  public void setMapValueObjectIDs(final Collection<ObjectID> objectIDs) {
    this.valueObjectIDs = objectIDs;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);
    Assert.assertTrue("It's not possible to have both an object ID of the map and the object IDs of the map values, "
                      + "it's one or the other and at least one of them is mandatory",
                      (mapObjectID != null && null == valueObjectIDs)
                          || (null == mapObjectID && valueObjectIDs != null));

    putNVPair(THREAD_ID, threadID.toLong());

    if (mapObjectID != null) {
      putNVPair(MAP_OBJECT_ID, mapObjectID.toLong());
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
      case MAP_OBJECT_ID:
        mapObjectID = new ObjectID(getLongValue());
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
}

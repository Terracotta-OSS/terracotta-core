/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ServerMapEvictionBroadcastMessageImpl extends DSOMessageBase implements ServerMapEvictionBroadcastMessage {

  private final static byte        MAP_OBJECT_ID     = 0;
  private final static byte        EVICTED_KEYS_SIZE = 1;
  private final static byte        CLIENT_INDEX      = 2;

  // TODO::Comeback and verify
  private static final DNAEncoding encoder           = new StorageDNAEncodingImpl();
  // Since ApplicatorDNAEncodingImpl is only available in the client, some tricker to get this reference set.
  private final DNAEncoding        decoder;

  private ObjectID                 mapID;
  private Set                      evictedKeys;
  private int                      clientIndex       = -1;

  public ServerMapEvictionBroadcastMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                               final MessageChannel channel, final TCMessageHeader header,
                                               final TCByteBuffer[] data, final DNAEncoding decoder) {
    super(sessionID, monitor, channel, header, data);
    this.decoder = decoder;
  }

  public ServerMapEvictionBroadcastMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                               final TCByteBufferOutputStream out, final MessageChannel channel,
                                               final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
    this.decoder = null; // shouldn't be used
  }

  public void initializeEvictionBroadcastMessage(final ObjectID mapObjectID, final Set evictedObjectKeys,
                                                 final int clientindex) {
    this.mapID = mapObjectID;
    this.evictedKeys = evictedObjectKeys;
    this.clientIndex = clientindex;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertTrue(this.clientIndex >= 0);
    putNVPair(CLIENT_INDEX, this.clientIndex);
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(EVICTED_KEYS_SIZE, this.evictedKeys.size());
    for (final Object evictedKey : this.evictedKeys) {
      encoder.encode(evictedKey, getOutputStream());
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case CLIENT_INDEX:
        this.clientIndex = getIntValue();
        Assert.assertTrue(this.clientIndex >= 0);
        return true;

      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;

      case EVICTED_KEYS_SIZE:
        int size = getIntValue();
        this.evictedKeys = new HashSet(size);
        final TCByteBufferInputStream inputStream = getInputStream();
        while (size-- > 0) {
          try {
            this.evictedKeys.add(this.decoder.decode(inputStream));
          } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);
          }
        }
        return true;
      default:
        return false;
    }
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  public Set getEvictedKeys() {
    return evictedKeys;
  }

  public int getClientIndex() {
    return this.clientIndex;
  }

}

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
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GetAllKeysServerMapResponseMessageImpl extends DSOMessageBase implements
    GetAllKeysServerMapResponseMessage {

  private final static byte        MAP_OBJECT_ID = 0;
  private final static byte        REQUEST_ID    = 1;
  private final static byte        GET_ALL_KEYS_SIZE = 2;

  private ObjectID                 mapID;
  private ServerMapRequestID       requestID;
  private Set                      keys;

  private static final DNAEncoding encoder       = new StorageDNAEncodingImpl();
  // Since ApplicatorDNAEncodingImpl is only available in the client, some tricker to get this reference set.
  private final DNAEncoding        decoder;

  public GetAllKeysServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                 final MessageChannel channel, final TCMessageHeader header,
                                                 final TCByteBuffer[] data, final DNAEncoding decoder) {
    super(sessionID, monitor, channel, header, data);
    this.decoder = decoder;
  }

  public GetAllKeysServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                 final TCByteBufferOutputStream out, final MessageChannel channel,
                                                 final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
    this.decoder = null; // shouldn't be used
  }

  public void initializeGetAllKeysResponse(ObjectID mapObjectID, ServerMapRequestID serverMapRequestID, Set set) {
    this.keys = set;
    this.mapID = mapObjectID;
    this.requestID = serverMapRequestID;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(REQUEST_ID, this.requestID.toLong());
    putNVPair(GET_ALL_KEYS_SIZE, this.keys.size());
    int count = 0;

    final TCByteBufferOutputStream outStream = getOutputStream();
    for (Iterator iter = this.keys.iterator(); iter.hasNext();) {
      encoder.encode(iter.next(), outStream);
      count++;
    }
    Assert.assertEquals(this.keys.size(), count);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;

      case REQUEST_ID:
        this.requestID = new ServerMapRequestID(getLongValue());
        return true;

      case GET_ALL_KEYS_SIZE:
        int size = getIntValue();
        this.keys = new HashSet((int)(size * 1.5));
        final TCByteBufferInputStream inputStream = getInputStream();
        while (size-- > 0) {
          try {
            final Object key = this.decoder.decode(inputStream);
            this.keys.add(key);
          } catch (ClassNotFoundException e) {
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

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Set getAllKeys() {
    return this.keys;
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_ALL_KEYS;
  }
  

}

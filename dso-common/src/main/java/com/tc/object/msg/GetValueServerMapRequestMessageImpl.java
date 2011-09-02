/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class GetValueServerMapRequestMessageImpl extends DSOMessageBase implements GetValueServerMapRequestMessage {

  private final static byte                                         REQUESTS_COUNT = 0;
  private final static byte                                         MAP_ID         = 1;

  private final static DNAEncoding                                  encoder        = new SerializerDNAEncodingImpl();
  private final static DNAEncoding                                  decoder        = new StorageDNAEncodingImpl();

  private final Map<ObjectID, Collection<ServerMapGetValueRequest>> requests       = new HashMap<ObjectID, Collection<ServerMapGetValueRequest>>();
  private int                                                       requestsCount;

  public GetValueServerMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                             final MessageChannel channel, final TCMessageHeader header,
                                             final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public GetValueServerMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                             final TCByteBufferOutputStream out, final MessageChannel channel,
                                             final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void addGetValueRequestTo(final ServerMapRequestID serverMapRequestID, final ObjectID id,
                                   final Set<Object> keys) {
    Collection<ServerMapGetValueRequest> requestsForMap = this.requests.get(id);
    if (requestsForMap == null) {
      requestsForMap = new ArrayList<ServerMapGetValueRequest>();
      this.requests.put(id, requestsForMap);
    }
    requestsForMap.add(new ServerMapGetValueRequest(serverMapRequestID, keys));
    this.requestsCount++;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REQUESTS_COUNT, this.requestsCount);
    int count = 0;
    final TCByteBufferOutputStream outStream = getOutputStream();
    for (final Entry<ObjectID, Collection<ServerMapGetValueRequest>> e : this.requests.entrySet()) {
      final ObjectID mapID = e.getKey();
      putNVPair(MAP_ID, mapID.toLong());
      final Collection<ServerMapGetValueRequest> requests4Map = e.getValue();
      // Directly encode the key
      outStream.writeInt(requests4Map.size());
      for (final ServerMapGetValueRequest svr : e.getValue()) {
        outStream.writeLong(svr.getRequestID().toLong());
        outStream.writeInt(svr.getKeys().size());
        for (Object key : svr.getKeys()) {
          encoder.encode(key, outStream);
        }
        count++;
      }
    }
    Assert.assertEquals(this.requestsCount, count);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case REQUESTS_COUNT:
        this.requestsCount = getIntValue();
        return true;

      case MAP_ID:
        final ObjectID mapID = new ObjectID(getLongValue());
        final Collection<ServerMapGetValueRequest> requests4Map = new ArrayList<ServerMapGetValueRequest>();
        int count = getIntValue();
        // Directly decode the key
        while (count-- > 0) {
          try {
            final long requestId = getLongValue();
            final int keySize = getIntValue();
            Set<Object> keys = new HashSet<Object>();
            for (int i = 0; i < keySize; i++) {
              keys.add(decoder.decode(getInputStream()));
            }
            requests4Map.add(new ServerMapGetValueRequest(new ServerMapRequestID(requestId), keys));
          } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);
          }
        }
        final Collection<ServerMapGetValueRequest> old = this.requests.put(mapID, requests4Map);
        Assert.assertNull(old);
        return true;

      default:
        return false;
    }
  }

  public Map<ObjectID, Collection<ServerMapGetValueRequest>> getRequests() {
    return this.requests;
  }

  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_VALUE_FOR_KEY;
  }

  public int getRequestCount() {
    return this.requestsCount;
  }
}

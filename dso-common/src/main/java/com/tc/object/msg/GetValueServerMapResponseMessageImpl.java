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
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class GetValueServerMapResponseMessageImpl extends DSOMessageBase implements GetValueServerMapResponseMessage {

  private final static byte                     MAP_OBJECT_ID  = 0;
  private final static byte                     RESPONSES_SIZE = 1;

  // TODO::Comeback and verify
  private static final DNAEncoding              encoder        = new StorageDNAEncodingImpl();
  // Since ApplicatorDNAEncodingImpl is only available in the client, some tricker to get this reference set.
  private final DNAEncoding                     decoder;

  private ObjectID                              mapID;
  private Collection<ServerMapGetValueResponse> responses;

  public GetValueServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                              final MessageChannel channel, final TCMessageHeader header,
                                              final TCByteBuffer[] data, final DNAEncoding decoder) {
    super(sessionID, monitor, channel, header, data);
    this.decoder = decoder;
  }

  public GetValueServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                              final TCByteBufferOutputStream out, final MessageChannel channel,
                                              final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
    this.decoder = null; // shouldn't be used
  }

  public void initializeGetValueResponse(final ObjectID mapObjectID,
                                         final Collection<ServerMapGetValueResponse> getValueResponses) {
    this.responses = getValueResponses;
    this.mapID = mapObjectID;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(RESPONSES_SIZE, this.responses.size());
    // Directly encode the values
    final TCByteBufferOutputStream outStream = getOutputStream();
    for (final ServerMapGetValueResponse r : this.responses) {
      outStream.writeLong(r.getRequestID().toLong());
      encoder.encode(r.getValue(), getOutputStream());
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;

      case RESPONSES_SIZE:
        int size = getIntValue();
        this.responses = new ArrayList<ServerMapGetValueResponse>();
        // Directly decode the value
        final TCByteBufferInputStream inputStream = getInputStream();
        while (size-- > 0) {
          try {
            this.responses.add(new ServerMapGetValueResponse(new ServerMapRequestID(getLongValue()), this.decoder
                .decode(inputStream)));
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

  public Collection<ServerMapGetValueResponse> getGetValueResponses() {
    return this.responses;
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_VALUE_FOR_KEY;
  }

}

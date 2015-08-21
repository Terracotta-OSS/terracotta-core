/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.CompoundResponse;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

public class GetValueServerMapResponseMessageImpl extends DSOMessageBase implements GetValueServerMapResponseMessage {

  private final static byte                     MAP_OBJECT_ID  = 0;
  private final static byte                     RESPONSES_SIZE = 1;
  private final static byte                     SERIALIZER_ID  = 2;
  private final static byte                     OBJECT_ID      = 3;
  private final static byte                     DNA_ID         = 4;

  // TODO::Comeback and verify
  private static final DNAEncoding              encoder        = new StorageDNAEncodingImpl();
  // Since ApplicatorDNAEncodingImpl is only available in the client, some tricker to get this reference set.
  private final DNAEncoding                     decoder;

  private ObjectID                              mapID;
  private Collection<ServerMapGetValueResponse> responses;
  private ObjectStringSerializer                serializer;

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

  @Override
  public void initializeGetValueResponse(final ObjectID mapObjectID,
                                         final ObjectStringSerializer serializer,
                                         final Collection<ServerMapGetValueResponse> getValueResponses) {
    this.responses = getValueResponses;
    this.serializer = serializer;
    this.mapID = mapObjectID;
  }

  // TODO: write a test to test hydrating and dehydrating
  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(SERIALIZER_ID, serializer);
    putNVPair(RESPONSES_SIZE, this.responses.size());
    // Directly encode the values
    final TCByteBufferOutputStream outStream = getOutputStream();
    for (final ServerMapGetValueResponse r : this.responses) {
      outStream.writeLong(r.getRequestID().toLong());
      outStream.writeInt(r.getValues().size());
      for (Entry<Object, Object> entry : r.getValues().entrySet()) {
        encoder.encode(entry.getKey(), outStream);
        CompoundResponse responseValue = (CompoundResponse)entry.getValue();
        Object value = responseValue.getData();
        if ( value instanceof ObjectID ) {
          outStream.writeByte(OBJECT_ID);
          outStream.writeLong(((ObjectID)value).toLong());
        } else {
          TCByteBufferOutputStream list = (TCByteBufferOutputStream)value;
          outStream.writeByte(DNA_ID);
          outStream.writeInt(list.getBytesWritten());
          outStream.write(list.toArray());
        }
        encoder.encode(responseValue.getCreationTime(), outStream);
        encoder.encode(responseValue.getLastAccessedTime(), outStream);
        encoder.encode(responseValue.getTimeToIdle(), outStream);
        encoder.encode(responseValue.getTimeToLive(), outStream);
        encoder.encode(responseValue.getVersion(), outStream);
      }
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer)getObject(new ObjectStringSerializerImpl());
        return true;
      case RESPONSES_SIZE:
        int size = getIntValue();
        this.responses = new ArrayList<ServerMapGetValueResponse>(size);
        // Directly decode the value
        final TCByteBufferInputStream inputStream = getInputStream();
        while (size-- > 0) {
          try {
            final long responseId = getLongValue();
            final int numResponses = getIntValue();
            ServerMapGetValueResponse response = new ServerMapGetValueResponse(new ServerMapRequestID(responseId));
            for (int i = 0; i < numResponses; i++) {
              Object key = decoder.decode(inputStream);
              byte type = inputStream.readByte();
              if ( type == OBJECT_ID ) {
                response.put(key, new ObjectID(inputStream.readLong()), false, (Long) decoder.decode(inputStream),
                             (Long) decoder.decode(inputStream), (Long) decoder.decode(inputStream),
                             (Long) decoder.decode(inputStream), (Long) decoder.decode(inputStream));
              } else {
                if ( type != DNA_ID ) {
                  throw new AssertionError("bad type");
                }
                int length = inputStream.readInt();
                byte[] grab = new byte[length];
                inputStream.readFully(grab);
                ObjectDNAImpl value = new ObjectDNAImpl(this.serializer, false);
                value.deserializeFrom(new TCByteBufferInputStream(TCByteBufferFactory.wrap(grab)));
                response.put(key, value.getObjectID(), true, (Long) decoder.decode(inputStream),
                             (Long) decoder.decode(inputStream), (Long) decoder.decode(inputStream),
                             (Long) decoder.decode(inputStream), (Long) decoder.decode(inputStream));
                response.replace(value.getObjectID(), value);
              }
            }
            this.responses.add(response);
          } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);
          }
        }
        return true;
      default:
        return false;
    }
  }

  @Override
  public ObjectID getMapID() {
    return this.mapID;
  }

  @Override
  public Collection<ServerMapGetValueResponse> getGetValueResponses() {
    return this.responses;
  }

  @Override
  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_VALUE_FOR_KEY;
  }

}

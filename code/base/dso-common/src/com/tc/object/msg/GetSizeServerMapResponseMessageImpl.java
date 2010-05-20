/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class GetSizeServerMapResponseMessageImpl extends DSOMessageBase implements GetSizeServerMapResponseMessage {

  private final static byte  MAP_OBJECT_ID = 0;
  private final static byte  REQUEST_ID    = 1;
  private final static byte  SIZE          = 2;

  private ObjectID           mapID;
  private ServerMapRequestID requestID;
  private Integer            size;

  public GetSizeServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                             final MessageChannel channel, final TCMessageHeader header,
                                             final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public GetSizeServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                             final TCByteBufferOutputStream out, final MessageChannel channel,
                                             final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initializeGetSizeResponse(final ObjectID mapObjectID, final ServerMapRequestID smRequestID,
                                        final Integer mapSize) {
    this.mapID = mapObjectID;
    this.requestID = smRequestID;
    this.size = mapSize;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(REQUEST_ID, this.requestID.toLong());
    putNVPair(SIZE, this.size);
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

      case SIZE:
        this.size = getIntValue();
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

  public Integer getSize() {
    return this.size;
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_SIZE;
  }
}

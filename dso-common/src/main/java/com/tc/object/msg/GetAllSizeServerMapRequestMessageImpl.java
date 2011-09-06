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
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class GetAllSizeServerMapRequestMessageImpl extends DSOMessageBase implements GetAllSizeServerMapRequestMessage {

  private final static byte  MAP_OBJECT_IDS = 0;
  private final static byte  REQUEST_ID     = 1;

  private ObjectID[]         mapIDs;
  private ServerMapRequestID requestID;

  public GetAllSizeServerMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                               final MessageChannel channel, final TCMessageHeader header,
                                               final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public GetAllSizeServerMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                               final TCByteBufferOutputStream out, final MessageChannel channel,
                                               final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initializeGetAllSizeRequest(final ServerMapRequestID serverMapRequestID, final ObjectID[] maps) {
    this.requestID = serverMapRequestID;
    this.mapIDs = maps;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REQUEST_ID, this.requestID.toLong());

    final TCByteBufferOutputStream outStream = getOutputStream();
    putNVPair(MAP_OBJECT_IDS, this.mapIDs.length);
    for (ObjectID oid : this.mapIDs) {
      outStream.writeLong(oid.toLong());
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_IDS:
        int length = getIntValue();
        this.mapIDs = new ObjectID[length];
        for (int i = 0; i < length; ++i) {
          this.mapIDs[i] = new ObjectID(getLongValue());
        }
        return true;

      case REQUEST_ID:
        this.requestID = new ServerMapRequestID(getLongValue());
        return true;

      default:
        return false;
    }
  }

  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  public ObjectID[] getMaps() {
    return this.mapIDs;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_SIZE;
  }

  public int getRequestCount() {
    return 1;
  }

}

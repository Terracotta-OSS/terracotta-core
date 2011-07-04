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

public class GetAllKeysServerMapRequestMessageImpl extends DSOMessageBase implements GetAllKeysServerMapRequestMessage {

  private final static byte  MAP_OBJECT_ID = 0;
  private final static byte  REQUEST_ID    = 1;

  private ObjectID           mapID;
  private ServerMapRequestID requestID;

  public GetAllKeysServerMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                            final MessageChannel channel, final TCMessageHeader header,
                                            final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public GetAllKeysServerMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                            final TCByteBufferOutputStream out, final MessageChannel channel,
                                            final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initializeSnapshotRequest(final ServerMapRequestID serverMapRequestID, final ObjectID id) {
    this.requestID = serverMapRequestID;
    this.mapID = id;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(REQUEST_ID, this.requestID.toLong());
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

      default:
        return false;
    }
  }

  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_ALL_KEYS;
  }

  public int getRequestCount() {
    return 1;
  }

}

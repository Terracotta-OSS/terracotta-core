/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class GetAllSizeServerMapResponseMessageImpl extends DSOMessageBase implements
    GetAllSizeServerMapResponseMessage {

  private final static byte  GROUP_ID   = 0;
  private final static byte  REQUEST_ID = 1;
  private final static byte  SIZE       = 2;

  private GroupID            groupID;
  private ServerMapRequestID requestID;
  private Long               size;

  public GetAllSizeServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                final MessageChannel channel, final TCMessageHeader header,
                                                final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public GetAllSizeServerMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                final TCByteBufferOutputStream out, final MessageChannel channel,
                                                final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initializeGetAllSizeResponse(final GroupID smGroupID, final ServerMapRequestID smRequestID,
                                           final Long mapSize) {
    this.groupID = smGroupID;
    this.requestID = smRequestID;
    this.size = mapSize;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(GROUP_ID, this.groupID.toInt());
    putNVPair(REQUEST_ID, this.requestID.toLong());
    putNVPair(SIZE, this.size);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case GROUP_ID:
        this.groupID = new GroupID(getIntValue());
        return true;

      case REQUEST_ID:
        this.requestID = new ServerMapRequestID(getLongValue());
        return true;

      case SIZE:
        this.size = getLongValue();
        return true;

      default:
        return false;
    }
  }

  public GroupID getGroupID() {
    return this.groupID;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Long getSize() {
    return this.size;
  }

  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_SIZE;
  }

  // XXX inherited but not used
  public ObjectID getMapID() {
    return null;
  }
}

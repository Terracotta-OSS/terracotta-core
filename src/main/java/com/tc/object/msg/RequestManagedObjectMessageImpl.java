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
import com.tc.object.EntityDescriptor;
import com.tc.object.ObjectRequestID;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class RequestManagedObjectMessageImpl extends DSOMessageBase implements RequestManagedObjectMessage {
  private final static byte ENTITY_REQUESTED = 1;
  private final static byte ENTITY_REMOVED = 2;
  private final static byte REQUEST_ID                 = 4;

  private EntityDescriptor requestedEntity;
  private EntityDescriptor removedEntity;
  private ObjectRequestID   requestID;

  public RequestManagedObjectMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                         MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestManagedObjectMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                         TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    if (null != this.requestedEntity) {
      putNVPair(ENTITY_REQUESTED, this.requestedEntity);
    }
    if (null != this.removedEntity) {
      putNVPair(ENTITY_REMOVED, this.removedEntity);
    }
    putNVPair(REQUEST_ID, this.requestID.toLong());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case ENTITY_REQUESTED:
        this.requestedEntity = EntityDescriptor.readFrom(getInputStream());
        return true;
      case ENTITY_REMOVED:
        this.removedEntity = EntityDescriptor.readFrom(getInputStream());
        return true;
      case REQUEST_ID:
        this.requestID = new ObjectRequestID(getLongValue());
        return true;
      default:
        return false;
    }
  }

  @Override
  public ObjectRequestID getRequestID() {
    return this.requestID;
  }

  @Override
  public EntityDescriptor getRequestedEntity() {
    return this.requestedEntity;
  }

  @Override
  public EntityDescriptor getRemovedEntity() {
    return this.removedEntity;
  }

  @Override
  public void initialize(ObjectRequestID rid, EntityDescriptor requested, EntityDescriptor removed) {
    this.requestID = rid;
    this.requestedEntity = requested;
    this.removedEntity = removed;
  }

  @Override
  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  @Override
  public Object getSchedulingKey() {
    return getSourceNodeID();
  }
}

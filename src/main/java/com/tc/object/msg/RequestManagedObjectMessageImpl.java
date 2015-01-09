/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectRequestID;
import com.tc.object.session.SessionID;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.io.IOException;

public class RequestManagedObjectMessageImpl extends DSOMessageBase implements EventContext,
    RequestManagedObjectMessage {
  private final static byte MANAGED_OBJECT_ID          = 1;
  private final static byte MANAGED_OBJECTS_REMOVED_ID = 2;
  private final static byte REQUEST_ID                 = 4;
  private final static byte REQUESTING_THREAD_NAME     = 6;

  private ObjectIDSet       objectIDs;
  private ObjectIDSet       removed;
  private ObjectRequestID   requestID;
  private String            threadName;

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
    putNVPair(MANAGED_OBJECT_ID, objectIDs);
    putNVPair(MANAGED_OBJECTS_REMOVED_ID, removed);
    putNVPair(REQUEST_ID, this.requestID.toLong());
    putNVPair(REQUESTING_THREAD_NAME, this.threadName);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case MANAGED_OBJECT_ID:
        this.objectIDs = (ObjectIDSet) getObject(new BasicObjectIDSet());
        return true;
      case MANAGED_OBJECTS_REMOVED_ID:
        this.removed = (ObjectIDSet) getObject(new BasicObjectIDSet());
        return true;
      case REQUEST_ID:
        this.requestID = new ObjectRequestID(getLongValue());
        return true;
      case REQUESTING_THREAD_NAME:
        this.threadName = getStringValue();
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
  public ObjectIDSet getRequestedObjectIDs() {
    return this.objectIDs;
  }

  @Override
  public ObjectIDSet getRemoved() {
    return this.removed;
  }

  @Override
  public void initialize(ObjectRequestID rid, ObjectIDSet requestedObjectIDs, ObjectIDSet removeObjects) {
    this.requestID = rid;
    this.objectIDs = requestedObjectIDs;
    this.removed = removeObjects;
    this.threadName = Thread.currentThread().getName();
  }

  @Override
  public String getRequestingThreadName() {
    return this.threadName;
  }

  @Override
  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  @Override
  public Object getKey() {
    return getSourceNodeID();
  }

  @Override
  public LOOKUP_STATE getLookupState() {
    return LOOKUP_STATE.CLIENT;
  }
}

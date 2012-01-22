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
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.session.SessionID;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class RequestManagedObjectMessageImpl extends DSOMessageBase implements EventContext,
    RequestManagedObjectMessage {
  private final static byte MANAGED_OBJECT_ID          = 1;
  private final static byte MANAGED_OBJECTS_REMOVED_ID = 2;
  private final static byte REQUEST_ID                 = 4;
  private final static byte REQUEST_DEPTH_ID           = 5;
  private final static byte REQUESTING_THREAD_NAME     = 6;

  private final ObjectIDSet objectIDs                  = new ObjectIDSet();
  private ObjectIDSet       removed                    = new ObjectIDSet();
  private ObjectRequestID   requestID;
  private int               requestDepth;
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
    for (Iterator i = this.objectIDs.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      putNVPair(MANAGED_OBJECT_ID, id.toLong());
    }

    for (Iterator i = this.removed.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      putNVPair(MANAGED_OBJECTS_REMOVED_ID, id.toLong());
    }
    putNVPair(REQUEST_ID, this.requestID.toLong());
    putNVPair(REQUEST_DEPTH_ID, this.requestDepth);
    putNVPair(REQUESTING_THREAD_NAME, this.threadName);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case MANAGED_OBJECT_ID:
        this.objectIDs.add(new ObjectID(getLongValue()));
        return true;
      case MANAGED_OBJECTS_REMOVED_ID:
        this.removed.add(new ObjectID(getLongValue()));
        return true;
      case REQUEST_ID:
        this.requestID = new ObjectRequestID(getLongValue());
        return true;
      case REQUEST_DEPTH_ID:
        this.requestDepth = getIntValue();
        return true;
      case REQUESTING_THREAD_NAME:
        this.threadName = getStringValue();
        return true;
      default:
        return false;
    }
  }

  public ObjectRequestID getRequestID() {
    return this.requestID;
  }

  public ObjectIDSet getRequestedObjectIDs() {
    return this.objectIDs;
  }

  public ObjectIDSet getRemoved() {
    return this.removed;
  }

  public void initialize(ObjectRequestID rid, Set<ObjectID> requestedObjectIDs, int depth, ObjectIDSet removeObjects) {
    this.requestID = rid;
    this.objectIDs.addAll(requestedObjectIDs);
    this.removed = removeObjects;
    this.requestDepth = depth;
    this.threadName = Thread.currentThread().getName();
  }

  public int getRequestDepth() {
    return this.requestDepth;
  }

  public String getRequestingThreadName() {
    return this.threadName;
  }

  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  public Object getKey() {
    return getSourceNodeID();
  }

  public LOOKUP_STATE getLookupState() {
    return LOOKUP_STATE.CLIENT;
  }
}

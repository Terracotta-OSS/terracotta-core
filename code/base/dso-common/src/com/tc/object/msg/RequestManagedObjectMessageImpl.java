/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author steve
 */
public class RequestManagedObjectMessageImpl extends DSOMessageBase implements EventContext,
    RequestManagedObjectMessage {
  private final static byte MANAGED_OBJECT_ID          = 1;
  private final static byte MANAGED_OBJECTS_REMOVED_ID = 2;
  private final static byte REQUEST_ID                 = 4;
  private final static byte REQUEST_DEPTH_ID           = 5;

  private Set               objectIDs                  = new HashSet();
  private Set               removed                    = new HashSet();
  private ObjectRequestID   requestID;
  private int               requestDepth;

  public RequestManagedObjectMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                         TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestManagedObjectMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                         TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    for (Iterator i = objectIDs.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      putNVPair(MANAGED_OBJECT_ID, id.toLong());
    }

    for (Iterator i = removed.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      putNVPair(MANAGED_OBJECTS_REMOVED_ID, id.toLong());
    }
    putNVPair(REQUEST_ID, requestID.toLong());
    putNVPair(REQUEST_DEPTH_ID, requestDepth);
  }

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
      default:
        return false;
    }
  }

  public ObjectRequestID getRequestID() {
    return requestID;
  }

  public Set getObjectIDs() {
    return objectIDs;
  }

  public Set getRemoved() {
    return removed;
  }

  public void initialize(ObjectRequestContext ctxt, Set oids, Set removedIDs) {
    this.requestID = ctxt.getRequestID();
    this.objectIDs.addAll(oids);
    this.removed = removedIDs;
    this.requestDepth = ctxt.getRequestDepth();
  }

  public int getRequestDepth() {
    return requestDepth;
  }
}
/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.util.ObjectIDSet;

public class TestRequestManagedObjectMessage implements RequestManagedObjectMessage {

  private ObjectIDSet removed;
  private ObjectIDSet objectIDs;

  public TestRequestManagedObjectMessage() {
    super();
  }

  @Override
  public ObjectRequestID getRequestID() {
    return null;
  }

  @Override
  public ObjectIDSet getRequestedObjectIDs() {
    return this.objectIDs;
  }

  public void setObjectIDs(ObjectIDSet IDs) {
    this.objectIDs = IDs;
  }

  @Override
  public ObjectIDSet getRemoved() {
    return this.removed;
  }

  public void setRemoved(ObjectIDSet rm) {
    this.removed = rm;
  }

  @Override
  public void initialize(ObjectRequestID rID, ObjectIDSet requestedObjectIDs, int requestDepth,
                         ObjectIDSet removeObjects) {
    //
  }

  @Override
  public void send() {
    //
  }

  @Override
  public MessageChannel getChannel() {
    return null;
  }

  @Override
  public NodeID getSourceNodeID() {
    return new ClientID(0);
  }

  @Override
  public int getRequestDepth() {
    return 400;
  }

  @Override
  public void recycle() {
    return;
  }

  @Override
  public String getRequestingThreadName() {
    return "TestThreadDummy";
  }

  @Override
  public LOOKUP_STATE getLookupState() {
    return LOOKUP_STATE.CLIENT;
  }

  @Override
  public ClientID getClientID() {
    return new ClientID(0);
  }

  @Override
  public Object getKey() {
    return null;
  }

}

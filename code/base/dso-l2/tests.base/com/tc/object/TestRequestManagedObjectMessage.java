/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.util.ObjectIDSet;

public class TestRequestManagedObjectMessage implements RequestManagedObjectMessage, EventContext {

  private ObjectRequestID requestID;
  private ObjectIDSet     removed;
  private MessageChannel  channel;
  private ObjectIDSet     objectIDs;

  public TestRequestManagedObjectMessage() {
    super();
  }

  public ObjectRequestID getRequestID() {
    return this.requestID;
  }

  public ObjectIDSet getRequestedObjectIDs() {
    return this.objectIDs;
  }

  public void setObjectIDs(ObjectIDSet IDs) {
    this.objectIDs = IDs;
  }

  public ObjectIDSet getRemoved() {
    return this.removed;
  }

  public void setRemoved(ObjectIDSet rm) {
    this.removed = rm;
  }

  public void initialize(ObjectRequestContext ctxt, ObjectIDSet oids, ObjectIDSet removedIDs) {
    //
  }

  public void send() {
    //      
  }

  public MessageChannel getChannel() {
    return this.channel;
  }

  public NodeID getSourceNodeID() {
    return new ClientID(new ChannelID(0));
  }

  public int getRequestDepth() {
    return 400;
  }

  public void recycle() {
    return;
  }

  public String getRequestingThreadName() {
    return "TestThreadDummy";
  }

  public boolean isServerInitiated() {
    return false;
  }

  public ClientID getClientID() {
    return new ClientID(new ChannelID(0));
  }
}

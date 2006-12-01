/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.RequestManagedObjectMessage;

import java.util.Collection;
import java.util.Set;

public class TestRequestManagedObjectMessage implements RequestManagedObjectMessage, EventContext {

  private ObjectRequestID requestID;
  private Set             removed;
  private ChannelID       channelID;
  private MessageChannel  channel;
  private Collection      objectIDs;

  public TestRequestManagedObjectMessage() {
    super();
  }

  public ObjectRequestID getRequestID() {
    return this.requestID;
  }

  public Collection getObjectIDs() {
    return this.objectIDs;
  }

  public void setObjectIDs(Collection IDs) {
    this.objectIDs = IDs;
  }

  public Set getRemoved() {
    return this.removed;
  }

  public void setRemoved(Set rm) {
    this.removed = rm;
  }

  public void initialize(ObjectRequestContext ctxt, Set oids, Set removedIDs) {
    //
  }

  public int getCorrelationId(boolean initialize) {
    return 0;
  }

  public void setCorrelationId(int id) {
    //
  }

  public TCMessageType getMessageType() {
    return null;
  }

  public void hydrate() {
    //
  }

  public void dehydrate() {
    //      
  }

  public void send() {
    //      
  }

  public MessageChannel getChannel() {
    return this.channel;
  }

  public ChannelID getChannelID() {
    return this.channelID;
  }

  public int getTotalLength() {
    return 0;
  }

  public int getRequestDepth() {
    return 400;
  }

  public void recycle() {
    return;
  }
}

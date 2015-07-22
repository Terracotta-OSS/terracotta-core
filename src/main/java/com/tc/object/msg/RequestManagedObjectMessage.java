/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;

public interface RequestManagedObjectMessage extends ObjectRequestContext, MultiThreadedEventContext, Recyclable {

  public EntityDescriptor getRemovedEntity();

  public void initialize(ObjectRequestID requestID, EntityDescriptor requestedEntity, EntityDescriptor removedEntity);

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();
}

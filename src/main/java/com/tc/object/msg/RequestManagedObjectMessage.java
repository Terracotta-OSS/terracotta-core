/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;

import java.util.Set;

public interface RequestManagedObjectMessage extends ObjectRequestServerContext, Recyclable {

  public Set<EntityDescriptor> getRemoved();

  public void initialize(ObjectRequestID requestID, Set<EntityDescriptor> requestedEntities,
                         Set<EntityDescriptor> removedEntities);

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();
}

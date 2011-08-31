/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public interface RequestManagedObjectMessage extends ObjectRequestServerContext, Recyclable, MultiThreadedEventContext {

  public ObjectIDSet getRemoved();

  public void initialize(ObjectRequestID requestID, Set<ObjectID> requestedObjectIDs, int requestDepth,
                         ObjectIDSet removeObjects);

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.util.ObjectIDSet;

public interface RequestManagedObjectMessage extends ObjectRequestServerContext, Recyclable {

  public ObjectIDSet getRemoved();

  public void initialize(ObjectRequestID requestID, ObjectIDSet requestedObjectIDs,
                         ObjectIDSet removeObjects);

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();

}

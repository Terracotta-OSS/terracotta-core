/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;
import com.tc.util.ObjectIDSet;

public interface RequestManagedObjectMessage extends Recyclable {

  public ObjectRequestID getRequestID();

  public ObjectIDSet getObjectIDs();

  public ObjectIDSet getRemoved();

  public String getRequestingThreadName();

  public void initialize(ObjectRequestContext ctxt, ObjectIDSet objectIDs, ObjectIDSet removedIDs);

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();

  public int getRequestDepth();

}

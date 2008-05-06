/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;

import java.util.Set;

public interface RequestManagedObjectMessage extends Recyclable {

  public ObjectRequestID getRequestID();

  public Set getObjectIDs();

  public Set getRemoved();
  
  public String getRequestingThreadName();

  public void initialize(ObjectRequestContext ctxt, Set objectIDs, Set removedIDs);

  public void send();

  public MessageChannel getChannel();

  public ClientID getClientID();

  public int getRequestDepth();

}

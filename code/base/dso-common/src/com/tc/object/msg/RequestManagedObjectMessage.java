/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;

import java.util.Collection;
import java.util.Set;

public interface RequestManagedObjectMessage extends Recyclable {

  public ObjectRequestID getRequestID();

  public Collection getObjectIDs();

  public Set getRemoved();

  public void initialize(ObjectRequestContext ctxt, Set objectIDs, Set removedIDs);

  public void send();

  public MessageChannel getChannel();

  public ChannelID getChannelID();

  public int getRequestDepth();

}

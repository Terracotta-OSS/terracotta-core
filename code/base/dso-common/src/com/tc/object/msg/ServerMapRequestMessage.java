/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ServerMapRequestType;

public interface ServerMapRequestMessage extends EventContext {

  public void send();

  public ClientID getClientID();

  public ServerMapRequestType getRequestType();

  public int getRequestCount();
  
  public MessageChannel getChannel();
}

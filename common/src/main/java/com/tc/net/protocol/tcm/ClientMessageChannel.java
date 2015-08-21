/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.object.ClientIDProvider;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;


public interface ClientMessageChannel extends MessageChannel, ClientIDProvider {

  public int getConnectCount();

  public int getConnectAttemptCount();

  public void reopen() throws Exception;

  public LockRequestMessageFactory getLockRequestMessageFactory();

  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory();

}

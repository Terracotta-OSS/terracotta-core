/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;

/**
 * Responsible for the delivery logic of messages
 */
interface DeliveryProtocol {

  public void send(TCNetworkMessage message);

  public void receive(OOOProtocolMessage protocolMessage);

  public void start();

  /**
   * call me when a connection is lost
   */
  public void pause();

  /**
   * call me when a connection is made
   */
  public void resume();

}
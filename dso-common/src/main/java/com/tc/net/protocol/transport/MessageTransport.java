/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.TCNetworkMessage;

import java.util.List;

/**
 * Interface for message transport layer-- the connection-side endcap to the message communications stack.
 */
public interface MessageTransport extends NetworkLayer {

  public static final int CONNWEIGHT_TX_HANDSHAKED = 1;

  public ConnectionID getConnectionId();

  public void addTransportListener(MessageTransportListener listener);

  public void addTransportListeners(List transportListeners);

  public void removeTransportListeners();

  public void attachNewConnection(TCConnection connection) throws IllegalReconnectException;

  public void receiveTransportMessage(WireProtocolMessage message);

  public void sendToConnection(TCNetworkMessage message);

  public void setAllowConnectionReplace(boolean allow);

  public short getCommunicationStackFlags(NetworkLayer parentLayer);

  public String getCommunicationStackNames(NetworkLayer parentLayer);

  public void setRemoteCallbackPort(int callbackPort);

  public int getRemoteCallbackPort();

  public void initConnectionID(ConnectionID cid);

}
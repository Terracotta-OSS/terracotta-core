/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  public void addTransportListeners(List<MessageTransportListener> transportListeners);

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

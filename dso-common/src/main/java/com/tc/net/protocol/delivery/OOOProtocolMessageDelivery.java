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
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;

public interface OOOProtocolMessageDelivery {

  public OOOProtocolMessage createHandshakeMessage(long ack);

  public OOOProtocolMessage createHandshakeReplyOkMessage(long ack);

  public OOOProtocolMessage createHandshakeReplyFailMessage(long ack);

  public OOOProtocolMessage createAckMessage(long sequence);

  public boolean sendMessage(OOOProtocolMessage msg);

  public void receiveMessage(OOOProtocolMessage msg);

  public OOOProtocolMessage createProtocolMessage(long sent, TCNetworkMessage msg);

  public ConnectionID getConnectionId();

}

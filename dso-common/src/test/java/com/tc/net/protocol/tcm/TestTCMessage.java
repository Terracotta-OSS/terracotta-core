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
package com.tc.net.protocol.tcm;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;

public class TestTCMessage implements TCMessage {

  public TCMessageType type = TCMessageType.PING_MESSAGE;

  public int getCorrelationId(boolean initialize) {
    return 0;
  }

  public void setCorrelationId(int id) {
    return;
  }

  @Override
  public TCMessageType getMessageType() {
    return type;
  }

  @Override
  public void hydrate() {
    return;
  }

  @Override
  public void dehydrate() {
    return;
  }

  @Override
  public void send() {
    return;
  }

  @Override
  public MessageChannel getChannel() {
    return null;
  }

  @Override
  public int getTotalLength() {
    return 100;
  }

  public ClientID getClientID() {
    return null;
  }

  public boolean resend() {
    throw new ImplementMe();

  }

  @Override
  public SessionID getLocalSessionID() {
    throw new ImplementMe();
  }

  @Override
  public NodeID getSourceNodeID() {
    throw new ImplementMe();
  }

  @Override
  public NodeID getDestinationNodeID() {
    throw new ImplementMe();
  }

}

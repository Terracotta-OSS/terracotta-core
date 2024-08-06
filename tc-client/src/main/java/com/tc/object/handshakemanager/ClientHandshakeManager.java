/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.handshakemanager;

import com.tc.object.msg.ClientHandshakeAckMessage;


public interface ClientHandshakeManager {
  /**
   * Called when the connection drops to notify the receiver that it should go into a paused state, unless this was expected
   * due to an actual shutdown operation.
   */
  public void disconnected();

  /**
   * Called when the connection is established to notify the receiver that it should now send a handshake over the wire and
   * can now start sending/receiving messages.  Note that the handshake will NOT be sent if the receiver is already in a
   * shutdown operation.
   */
  public void connected();

  /**
   * Called when an attempt to reconnect to the server fails.
   */
  public void fireNodeError();

  /**
   * Called with the ACK from a formerly sent handshake message.
   * This includes information describing the capabilities of the server, such as whether or not it is persistent.
   * 
   * @param handshakeAck
   */
  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck);

  /**
   * Blocks the caller until an acknowledgement of the handshake has been received, changing the receiver into a running
   * state.
   */
  public void waitForHandshake();

  /**
   * Called to start a shutdown operation.
   * 
   */
  public void shutdown();

  /**
   * @return True if shutdown(boolean) has been called.  False, otherwise.
   */
  public boolean isShutdown();
}

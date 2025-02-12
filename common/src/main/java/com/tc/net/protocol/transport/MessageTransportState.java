/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.net.protocol.transport;

enum MessageTransportState {
  /**
   * XXX: move to client state machine Initial state for client transports.
   */
  STATE_START("START") {
    @Override
    public boolean isAlive() {
      return false;
    }
  },
    
  STATE_CONNECTED("CONNECTED"),
  
  STATE_RESTART("RESTART") {
    @Override
    public boolean isAlive() {
      return false;
    }
  },

  /**
   * XXX: Move to client state machine SYN message sent, waiting for reply
   */
  STATE_SYN_SENT("SYN_SENT"),
  // /**
  // * XXX: Move to server state machine
  // */
  // TATE_SYN_ACK_SENT("SYN_ACK_SENT"),
  /**
   * The client sends a SYN with a connection id that we can't reconnect to (e.g. we can't find the corresponding
   * server-side stack).
   */
  STATE_SYN_ACK_ERROR("SYN_ACK_ERROR"),

  /**
   * XXX: Move to client state machine SYN_ACK received-- we're ready to talk!
   */
  STATE_ESTABLISHED("ESTABLISHED"),

  // Transport got disconnected -- probably health checker didnt like the client
  STATE_DISCONNECTED("DISCONNECTED"),

  /**
   * End state-- if the client is disconnected and isn't going to reconnect or if there is a handshake error (server or
   * client)
   */
  STATE_END("END") {
    @Override
    public boolean isAlive() {
      return false;
    }
  };

  private final String name;

  private MessageTransportState(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
  
  public boolean isAlive() {
    return true;
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public enum MessageTransportState {
  /**
   * XXX: move to client state machine Initial state for client transports.
   */
  STATE_START("START"),

  STATE_RESTART("RESTART"),

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

  // Transport Closed
  STATE_CLOSED("CLOSED"),

  // Transport got disconnected -- probably health checker didnt like the client
  STATE_DISCONNECTED("DISCONNECTED"),

  /**
   * End state-- if the client is disconnected and isn't going to reconnect or if there is a handshake error (server or
   * client)
   */
  STATE_END("END");

  private final String name;

  private MessageTransportState(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
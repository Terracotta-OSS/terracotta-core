/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class MessageTransportState {
  /**
   * XXX: move to client state machine Initial state for client transports.
   */
  public static final MessageTransportState STATE_START         = new MessageTransportState("START");

  public static final MessageTransportState STATE_RESTART       = new MessageTransportState("RESTART");

  /**
   * XXX: Move to client state machine SYN message sent, waiting for reply
   */
  public static final MessageTransportState STATE_SYN_SENT      = new MessageTransportState("SYN_SENT");
  // /**
  // * XXX: Move to server state machine
  // */
  // public static final MessageTransportState STATE_SYN_ACK_SENT = new
  // MessageTransportState("SYN_ACK_SENT");
  /**
   * The client sends a SYN with a connection id that we can't reconnect to (e.g. we can't find the corresponding
   * server-side stack).
   */
  public static final MessageTransportState STATE_SYN_ACK_ERROR = new MessageTransportState("SYN_ACK_ERROR");

  /**
   * XXX: Move to client state machine SYN_ACK received-- we're ready to talk!
   */
  public static final MessageTransportState STATE_ESTABLISHED   = new MessageTransportState("ESTABLISHED");

  // Transport Closed
  public static final MessageTransportState STATE_CLOSED        = new MessageTransportState("CLOSED");

  // Transport got disconnected -- probably health checker didnt like the client
  public static final MessageTransportState STATE_DISCONNECTED  = new MessageTransportState("DISCONNECTED");

  /**
   * End state-- if the client is disconnected and isn't going to reconnect or if there is a handshake error (server or
   * client)
   */
  public static final MessageTransportState STATE_END           = new MessageTransportState("END");

  private final String                      name;

  private MessageTransportState(String name) {
    this.name = name;
  }

  public String toString() {
    return this.name;
  }
}
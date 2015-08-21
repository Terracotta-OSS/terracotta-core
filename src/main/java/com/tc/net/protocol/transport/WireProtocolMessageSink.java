/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Interface for objects that receive/consume TC Wire protocol Messages
 * 
 * @author teck
 */
public interface WireProtocolMessageSink {

  /**
   * Inject the given wire protocol message. Implementations are free to queue the message (and return) or act on it
   * directly in the context of the current thread
   * 
   * @param message The message instance to put
   */
  public void putMessage(WireProtocolMessage message) throws WireProtocolException;

  //  /**
  //   * Inject the given wire protocol messages. Implementations are free to queue the messages (and return)
  //   * or act on them directly in the context of the current thread
  //   *
  //   * @param messages The message instances to put
  //   */
  //  public void putMessages(TCWireProtocolMessage[] messages);
}

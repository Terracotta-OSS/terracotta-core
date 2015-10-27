/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

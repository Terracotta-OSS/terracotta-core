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

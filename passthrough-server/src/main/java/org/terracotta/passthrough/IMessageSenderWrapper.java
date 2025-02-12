/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

/**
 * An interface used to represent the party who sent a message to a server.
 * This exists so that the core message handling code can work the same way, whether the sender of the message was a client
 * or an active.
 * Specifically, the meaning of different messages is exposed via different methods since some implementations don't want to
 * use the message, only understand why it was sent.
 */
public interface IMessageSenderWrapper {
  default void open() {
    
  }
  void sendAck(PassthroughMessage ack);
  void sendComplete(PassthroughMessage complete, boolean monitor);
  void sendRetire(PassthroughMessage retire);
  PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID);
  /**
   * Used for identifying a PassthroughConnection or anything which wraps one as an IMessageSenderWrapper.  This allows for
   * unique identification of something which represents a client.
   * @return The client origin unique ID.
   */
  long getClientOriginID();
  default void close() {
    
  }
}

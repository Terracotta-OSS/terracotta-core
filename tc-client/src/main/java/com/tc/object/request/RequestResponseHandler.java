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
package com.tc.object.request;

import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;


/**
 * Note that the methods in this interface can be called on multiple threads, concurrently, so the implementation must
 * synchronize where appropriate.
 */
public interface RequestResponseHandler {
  /**
   * Called when the server sends back the RECEIVED acknowledgement.
   */
  void received(TransactionID id);

  /**
   * Called when the server sends back a response that the request completed successfully, with no return value.  This
   * implies an COMPLETED acknowledgement.
   */
  void complete(TransactionID id);

  /**
   * Called when the server sends back a response that the request completed successfully, with a return value.  This
   * implies an COMPLETED acknowledgement.
   */
  void complete(TransactionID id, byte[] value);

  /**
   * Called when the server sends back a response that the request completed with a failure, as described by the
   * exception.  This implies an COMPLETED acknowledgement.  Note that all of our wire-level exceptions are now
   * EntityException instances.
   */
  void failed(TransactionID id, Exception e);

  /**
   * Called when the server sends back a response that the message no longer needs to be tracked, at all, and can be retired.
   * This is the last message in the sequence, coming after either a "complete" or "failed".
   */
  void retired(TransactionID id);
  
  /**
   * Handles a message received from the server. It will hand off the message to the client side entity if it exists.
   * otherwise it'll drop the message on the floor.
   *
   * @param clientID the instance to receive the message.
   * @param message opaque message
   */
  void handleMessage(ClientInstanceID clientID, byte[] message);
  /**
   * Handles a message received from the server. It will hand off the message to the client side entity if it exists.
   * otherwise it'll drop the message on the floor.
   *
   * @param transaction the inflight message to receive the message.
   * @param message opaque message
   */
  void handleMessage(TransactionID transaction, byte[] message);
  
  void handleStatistics(TransactionID transaction, long[] message);
}

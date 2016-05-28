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

package com.tc.object.request;

import org.terracotta.exception.EntityException;
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
   * implies an APPLIED acknowledgement.
   */
  void complete(TransactionID id);

  /**
   * Called when the server sends back a response that the request completed successfully, with a return value.  This
   * implies an APPLIED acknowledgement.
   */
  void complete(TransactionID id, byte[] value);

  /**
   * Called when the server sends back a response that the request completed with a failure, as described by the
   * exception.  This implies an APPLIED acknowledgement.  Note that all of our wire-level exceptions are now
   * EntityException instances.
   */
  void failed(TransactionID id, EntityException e);

  /**
   * Called when the server sends back a response that the message no longer needs to be tracked, at all, and can be retired.
   * This is the last message in the sequence, coming after either a "complete" or "failed".
   */
  void retired(TransactionID id);
}

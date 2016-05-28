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

package com.tc.entity;

import org.terracotta.entity.EntityMessage;

import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;


public interface VoltronEntityMessage {
  enum Type {
    /**
     * Called to add a reference from a client to an existing entity.
     */
    FETCH_ENTITY,
    /**
     * Called to release a reference from a client to an existing entity (previously acquired with FETCH_ENTITY).
     */
    RELEASE_ENTITY,
    /**
     * Used when checking if an entity exists but has no side-effects on the entity or client-entity references.
     */
    DOES_EXIST,
    /**
     * Creates a new entity.  This doesn't create a reference from the caller to the entity, however.
     */
    CREATE_ENTITY,
    /**
     * Called to destroy an existing entity (previously created with CREATE_ENTITY).
     */
    DESTROY_ENTITY,
    /**
     * Called to recreate an active entity with a new configuration
     */
    RECONFIGURE_ENTITY,
    /**
     * Used when invoking a method on an existing entity.
     */
    INVOKE_ACTION,
    /**
     * noop for pipeline flushes
     */
    NOOP
  }
  
  enum Acks {
    /**
     * Sent when the local client has determined that the message is now "in-flight".  This means that a reconnect during
     * send will result in this message being re-sent as the reconnect handshake, as opposed to being sent as a new
     * transaction.
     */
    SENT,
    /**
     * Sent when the active receives the message and after it has enqueued it for execution in the entity.
     */
    RECEIVED,
    /**
     * Sent with the actual response.  Once this is received, the get() on the future will return the value/exception
     * from the server.
     * If this Ack is not requested, the get() will only return any local exceptions and no return value (null).
     */
    APPLIED,
    /**
     * Sent AFTER the APPLIED response.  Even if APPLIED is received, it is still possible that a reconnect could cause the
     * message to re-send until the RETIRED is received.
     * While this ACK typically arrives immediately after APPLIED, it can come much later if EntityMessenger is used to
     * defer the retirement of a completed message.
     */
    RETIRED,
  }
  
  ClientID getSource();
  
  TransactionID getTransactionID();
  
  EntityDescriptor getEntityDescriptor();

  boolean doesRequireReplication();
  
  Type getVoltronType();
  
  byte[] getExtendedData();
  
  /**
   * This represents the oldest transaction that the sending client still knows about, from a tracking perspective.  The
   * client will clear this once it gets the final APPLED response from the server but, in the meantime, the server must
   * remember the order of transactions from this client going back at least as far as this transaction, to preserve
   * re-send order in the case of a restart or fail-over.
   */
  TransactionID getOldestTransactionOnClient();
  
  /**
   * Provided for the cases where an entity message instance already exists.  Note that getExtendedData is expected to
   * return a serialized version of this message, if it isn't null.
   * 
   * @return The EntityMessage instance or null, if there isn't one.
   */
  public EntityMessage getEntityMessage();
}

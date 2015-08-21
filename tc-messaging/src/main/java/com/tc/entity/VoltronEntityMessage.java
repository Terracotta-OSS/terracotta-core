package com.tc.entity;

import com.tc.net.NodeID;
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
     * Used when invoking a method on an existing entity.
     */
    INVOKE_ACTION,
  }
  
  enum Acks {
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
  }
  
  NodeID getSource();
  
  TransactionID getTransactionID();
  
  EntityDescriptor getEntityDescriptor();

  boolean doesRequireReplication();
  
  Type getType();
  
  byte[] getExtendedData();
  
  /**
   * This represents the oldest transaction that the sending client still knows about, from a tracking perspective.  The
   * client will clear this once it gets the final APPLED response from the server but, in the meantime, the server must
   * remember the order of transactions from this client going back at least as far as this transaction, to preserve
   * re-send order in the case of a restart or fail-over.
   */
  TransactionID getOldestTransactionOnClient();
}

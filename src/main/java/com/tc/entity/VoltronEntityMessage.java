package com.tc.entity;

import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;

import java.util.Set;


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
    RECEIPT,
    PERSIST_IN_SEQUENCER,
    REPLICATED,
    APPLIED
  }
  
  NodeID getSource();
  
  TransactionID getTransactionID();
  
  EntityDescriptor getEntityDescriptor();

  Set<Acks> getAcks();
  
  Type getType();
  
  byte[] getExtendedData();
}

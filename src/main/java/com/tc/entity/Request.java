package com.tc.entity;

import com.tc.net.NodeID;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;

import java.util.Set;

/**
 * @author twu
 */
public interface Request {
  enum Type {
    CREATE_ENTITY,
    INVOKE_ACTION,
    GET_ENTITY,
    DELETE_ENTITY
  }
  
  enum Acks {
    RECEIPT,
    PERSIST_IN_SEQUENCER,
    REPLICATED,
    APPLIED
  }
  
  NodeID getSource();
  
  TransactionID getTransactionID();
  
  EntityID getEntityID();
  
  Set<Acks> getAcks();
  
  Type getType();
  
  byte[] getPayload();
}

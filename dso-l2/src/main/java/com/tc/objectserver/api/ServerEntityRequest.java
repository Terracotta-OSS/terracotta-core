package com.tc.objectserver.api;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.exception.EntityException;

import com.tc.net.NodeID;
import com.tc.object.tx.TransactionID;

public interface ServerEntityRequest {

  ServerEntityAction getAction();

  NodeID getNodeID();
  
  TransactionID getTransaction();
  
  TransactionID getOldestTransactionOnClient();
  /**
   * The descriptor referring to the specific client-side object instance which issued the request.
   */
  ClientDescriptor getSourceDescriptor();

  byte[] getPayload();

  void complete();

  void complete(byte[] value);

  void failure(EntityException e);

  void received();

  boolean requiresReplication();
}

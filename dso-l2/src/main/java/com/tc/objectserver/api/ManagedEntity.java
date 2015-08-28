package com.tc.objectserver.api;

import org.terracotta.entity.ClientDescriptor;
import com.tc.net.NodeID;
import com.tc.object.EntityID;


/**
 * Contains a managed entity or holds a place for a specific entity yet-to-be-created.
 * The ProcessTransactionHandler passes requests into this to be applied to the underlying entity.
 * Additionally, client-entity connections are rebuilt, after reconnect, using this interface.
 */
public interface ManagedEntity {
  public EntityID getID();
  
  public void addRequest(ServerEntityRequest request);
  
  /**
   * Called to handle the reconnect for a specific client instance living on a specific node.
   * This is called after restart or fail-over to re-associate a formerly connected client with its server-side entities.
   * Note that this call is made BEFORE any re-sent transactions are issued to the entity.
   * 
   * @param nodeID The client node involved in the reconnect
   * @param clientDescriptor The specific instance on that client which is requesting to reconnect
   * @param extendedReconnectData Free-formed data sent by the client to help restore the in-memory state of the entity
   */
  public void reconnectClient(NodeID nodeID, ClientDescriptor clientDescriptor, byte[] extendedReconnectData);
}

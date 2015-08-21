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
  
  public void reconnectClient(NodeID nodeID, ClientDescriptor clientDescriptor);
}

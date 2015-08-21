/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.entity;

import org.terracotta.entity.ClientDescriptor;

import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;

/**
 * An opaque token representing a specific entity instance on a specific client node.
 * This is used by server-side code to specifically communicate with or track connection status of a specific client-side
 * object instance.
 * Compared to EntityDescriptor, the EntityDescriptor can uniquely identify a client-side entity instance within the context
 * of the node where it lives.  A ClientDescriptor, on the other hand, has global scope:  it also knows the node where the
 * client-side entity lives.
 */
public class ClientDescriptorImpl implements ClientDescriptor {
  // The specific node where the referenced instance lives.
  private final NodeID nodeID;
  private final EntityDescriptor entityDescriptor;
  
  public ClientDescriptorImpl(NodeID nodeID, EntityDescriptor entityDescriptor) {
    this.nodeID = nodeID;
    this.entityDescriptor = entityDescriptor;
  }
  
  public NodeID getNodeID() {
    return this.nodeID;
  }
  
  public EntityDescriptor getEntityDescriptor() {
    return this.entityDescriptor;
  }

  @Override
  public String toString() {
    return "ClientDescriptorImpl{" +
           "nodeID=" + nodeID +
           ", entityDescriptor=" + entityDescriptor +
           "}";
  }
  
  @Override
  public int hashCode() {
    return this.nodeID.hashCode() ^ this.entityDescriptor.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    boolean doesMatch = (this == other);
    if (!doesMatch && (getClass() == other.getClass()))
    {
      final ClientDescriptorImpl that = (ClientDescriptorImpl) other;
      doesMatch = this.nodeID.equals(that.nodeID)
          && this.entityDescriptor.equals(that.entityDescriptor);
    }
    return doesMatch;
  }
}

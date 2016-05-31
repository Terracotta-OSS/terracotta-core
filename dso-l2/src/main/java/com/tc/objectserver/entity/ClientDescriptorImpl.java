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
package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import org.terracotta.entity.ClientDescriptor;

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
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    final ClientDescriptorImpl that = (ClientDescriptorImpl) other;
    return this.nodeID.equals(that.nodeID) && this.entityDescriptor.equals(that.entityDescriptor);
  }
}

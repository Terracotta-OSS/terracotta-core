/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.util.Assert;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;

/**
 * An opaque token representing a specific entity instance on a specific client node.
 * This is used by server-side code to specifically communicate with or track connection status of a specific client-side
 * object instance.
 * Compared to EntityDescriptor, the EntityDescriptor can uniquely identify a client-side entity instance within the context
 * of the node where it lives.  A ClientDescriptor, on the other hand, has global scope:  it also knows the node where the
 * client-side entity lives.
 */
public class ClientDescriptorImpl implements ClientDescriptor {
  public static ClientDescriptorImpl NULL_ID = new ClientDescriptorImpl(ClientID.NULL_ID, ClientInstanceID.NULL_ID);
  // The specific node where the referenced instance lives.
  private final ClientID nodeID;
  private final ClientInstanceID clientInstance;

  public ClientDescriptorImpl(ClientID nodeID, ClientInstanceID clientInstance) {
    Assert.assertNotNull(nodeID);
    Assert.assertNotNull(clientInstance);
    this.nodeID = nodeID;
    this.clientInstance = clientInstance;
  }

  public ClientDescriptorImpl() {
    this(ClientID.NULL_ID, ClientInstanceID.NULL_ID);
  }

  public ClientID getNodeID() {
    return this.nodeID;
  }
  
  public ClientInstanceID getClientInstanceID() {
    return this.clientInstance;
  }

  @Override
  public String toString() {
    return "ClientDescriptorImpl{" +
           "nodeID=" + nodeID +
           ", entityDescriptor=" + clientInstance +
           "}";
  }
  
  @Override
  public int hashCode() {
    return this.nodeID.hashCode() ^ this.clientInstance.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    boolean doesMatch = (this == other);
    if (!doesMatch && other != null && (getClass() == other.getClass()))
    {
      final ClientDescriptorImpl that = (ClientDescriptorImpl) other;
      doesMatch = this.nodeID.equals(that.nodeID)
          && this.clientInstance.equals(that.clientInstance);
    }
    return doesMatch;
  }

  public boolean isValid() {
    return !nodeID.isNull() && clientInstance.getID() > 0;
  }

  @Override
  public boolean isValidClient() {
    return isValid();
  }

  @Override
  public ClientSourceId getSourceId() {
    return new ClientSourceIdImpl(nodeID.toLong());
  }
}

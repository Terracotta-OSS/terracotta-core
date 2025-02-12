/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;



/**
 * The ClientDescriptor implementation used is very simple since it can carry the reference to the sender, directly.
 * This is used, by the server-side entities, to identify a specific client-side entity instance connected to it or sending
 * it messages.
 */
public class PassthroughClientDescriptor implements ClientDescriptor {
  public final PassthroughServerProcess server;
  public PassthroughConnection sender;
  public long clientInstanceID;

  public PassthroughClientDescriptor(PassthroughServerProcess server, PassthroughConnection sender, long clientInstanceID) {
    this.server = server;
    this.sender = sender;
    this.clientInstanceID = clientInstanceID;
  }

  @Override
  public int hashCode() {
    int result = sender != null ? Long.hashCode(sender.getUniqueConnectionID()) : 0;
    result = 31 * result + Long.hashCode(clientInstanceID);
    return result;
  }

  @Override
  public boolean isValidClient() {
    return sender != null;
  }

  @Override
  public ClientSourceId getSourceId() {
    return new PassthroughClientSourceId(sender == null ? -1 : sender.getUniqueConnectionID());
  }

  @Override
  public boolean equals(Object obj) {
    boolean isEqual = (obj == this);
    if (!isEqual && (obj instanceof PassthroughClientDescriptor)) {
      PassthroughClientDescriptor other = (PassthroughClientDescriptor) obj;
      // We can use instance compare, here, on the sender.
      isEqual = (other.sender.getUniqueConnectionID() == this.sender.getUniqueConnectionID())
          && (other.clientInstanceID == this.clientInstanceID);
    }
    return isEqual;
  }

  @Override
  public String toString() {
    String ret = "PassthroughClientDescriptor{server=";
    ret = ret + (server != null ? server.toString() : "<no server>");
    ret = ret + "," + (sender != null ? sender.toString() : "<no sender>");
    ret = ret + ",clientInstanceID=" + clientInstanceID + '}';
    return ret;
  }

}

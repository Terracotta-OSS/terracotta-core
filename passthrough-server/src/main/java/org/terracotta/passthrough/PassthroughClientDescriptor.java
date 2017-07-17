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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.terracotta.entity.ClientDescriptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


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

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeLong(sender.getUniqueConnectionID());
    out.writeLong(clientInstanceID);
  }

  private void readObject(ObjectInputStream in) throws IOException {
    this.sender = new PassthroughConnection(null, "", null, null, null, in.readLong());
    this.clientInstanceID = in.readLong();
  }
}

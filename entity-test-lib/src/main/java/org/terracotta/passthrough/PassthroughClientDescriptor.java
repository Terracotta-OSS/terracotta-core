package org.terracotta.passthrough;

import org.terracotta.entity.ClientDescriptor;


public class PassthroughClientDescriptor implements ClientDescriptor {
  public final PassthroughConnection sender;
  public final long clientInstanceID;

  public PassthroughClientDescriptor(PassthroughConnection sender, long clientInstanceID) {
    this.sender = sender;
    this.clientInstanceID = clientInstanceID;
  }

  @Override
  public int hashCode() {
    return this.sender.hashCode() ^ (int)this.clientInstanceID;
  }

  @Override
  public boolean equals(Object obj) {
    boolean isEqual = (obj == this);
    if (!isEqual && (obj instanceof PassthroughClientDescriptor)) { 
      PassthroughClientDescriptor other = (PassthroughClientDescriptor) obj;
      // We can use instance compare, here, on the sender.
      isEqual = (other.sender == this.sender)
          && (other.clientInstanceID == this.clientInstanceID);
    }
    return isEqual;
  }
}

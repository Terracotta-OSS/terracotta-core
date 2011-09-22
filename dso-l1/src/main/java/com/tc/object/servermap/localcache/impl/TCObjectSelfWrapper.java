/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.TCObjectSelfStoreValue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TCObjectSelfWrapper implements TCObjectSelfStoreValue, Externalizable {
  private volatile Object tcObject;

  public TCObjectSelfWrapper() {
    //
  }

  public TCObjectSelfWrapper(Object tcObject) {
    this.tcObject = tcObject;
  }

  public Object getTCObjectSelf() {
    return tcObject;
  }

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    tcObject = in.readObject();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(tcObject);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((tcObject == null) ? 0 : tcObject.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TCObjectSelfWrapper other = (TCObjectSelfWrapper) obj;
    if (tcObject == null) {
      if (other.tcObject != null) return false;
    } else if (!tcObject.equals(other.tcObject)) return false;
    return true;
  }

}
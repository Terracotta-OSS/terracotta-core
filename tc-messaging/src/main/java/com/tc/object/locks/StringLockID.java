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
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;

public class StringLockID implements LockID {
  private static final long        serialVersionUID = 0x159578a476cef87dL;

  private String                   id;

  public StringLockID() {
    // to make TCSerializable happy
  }

  /**
   * New id
   * 
   * @param id ID value
   */
  public StringLockID(String id) {
    Assert.eval(id != null);
    this.id = id;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + this.id + ")";
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StringLockID) {
      final StringLockID lid = (StringLockID) obj;
      return this.id.equals(lid.id);
    }
    return false;
  }

  @Override
  public int compareTo(LockID o) {
    if (o instanceof StringLockID) {
      StringLockID other = (StringLockID) o;
      return id.compareTo(other.id);
    }
    
    return toString().compareTo(o.toString());
  }

  @Override
  public StringLockID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readString();
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(this.id);
  }

  @Override
  public LockIDType getLockType() {
    return LockIDType.STRING;
  }
}

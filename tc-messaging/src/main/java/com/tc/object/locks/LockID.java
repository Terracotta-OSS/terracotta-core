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

import com.tc.io.TCSerializable;

import java.io.Serializable;

/**
 * Terracotta locks are taken on instances implementing LockID.
 * <p>
 * LockID implementations must implement this interface and be well behaved Map key types. That this must have equals
 * and hashCode methods that honor the JDK contracts.
 */
public interface LockID extends TCSerializable<LockID>, Serializable, Comparable<LockID> {
  /**
   * Enum of all known LockID types - this is used in TCSerialization code
   */
  static enum LockIDType {
    ENTITY, STRING, LONG,
  }

  /**
   * Returns the type of this LockID
   * <p>
   * Used to determine the TCSerialization format that should be used when sending over the network.
   */
  public LockIDType getLockType();
}

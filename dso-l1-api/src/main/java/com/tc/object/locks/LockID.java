/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
public interface LockID extends TCSerializable, Serializable, Comparable {
  /**
   * Enum of all known LockID types - this is used in TCSerialization code
   */
  static enum LockIDType {
    STRING, LONG, DSO, DSO_LITERAL, DSO_VOLATILE;
  }

  /**
   * Returns the type of this LockID
   * <p>
   * Used to determine the TCSerialization format that should be used when sending over the network.
   */
  public LockIDType getLockType();
}

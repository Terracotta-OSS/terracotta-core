/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import java.io.Serializable;

import com.tc.util.Assert;

/**
 * Identifier for a given lock
 * 
 * @author steve
 */
public class LockID implements Serializable {
  /** Indicates no lock identifier */
  public final static LockID NULL_ID = new LockID("null id");
  private final String       id;
  
  /**
   * New id
   * @param id ID value
   */
  public LockID(String id) {
    Assert.eval(id != null);
    this.id = id;
  }

  /**
   * @return True if is null identifier
   */
  public boolean isNull() {
    return this == NULL_ID;
  }

  /**
   * @return ID type
   */
  public String getIdentifierType() {
    return "LockID";
  }

  /**
   * @return String value of id value
   */
  public String asString() {
    return id;
  }

  public String toString() {
    return getIdentifierType() + "(" + id + ")";
  }

  public int hashCode() {
    return id.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof LockID) {
      LockID lid = (LockID) obj;
      return this.id.equals(lid.id);
    }
    return false;
  }
}

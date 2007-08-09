/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  public final static LockID NULL_ID = new LockID("null id");
  private final String       id;

  public LockID(String id) {
    Assert.eval(id != null);
    this.id = id;
  }

  public boolean isNull() {
    return this == NULL_ID;
  }

  public String getIdentifierType() {
    return "LockID";
  }

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

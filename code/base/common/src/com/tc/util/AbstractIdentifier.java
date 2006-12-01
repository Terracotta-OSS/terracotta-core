/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.Serializable;

/**
 * Generic Identifier class. Currently used as superclass for ObjectID, TransactionID and ChannelID
 * 
 * @author steve
 */
public abstract class AbstractIdentifier implements Serializable {
  private static final long NULL_ID = -1;
  private final long        id;

  public AbstractIdentifier(long id) {
    this.id = id;
//    this is done to remove an unnecessary boolean to identify NULL Id for performance reason.
//    if(id == NULL_ID) {
//      throw new AssertionError("ID can't be -1");
//    }
  }

  protected AbstractIdentifier() {
    this.id = NULL_ID;
  }

  public boolean isNull() {
    return (this.id == NULL_ID);
  }

  public final long toLong() {
    return id;
  }

  public final String toString() {
    return getIdentifierType() + "=" + "[" + id + "]";
  }

  abstract public String getIdentifierType();

  public int hashCode() {
    return (int) (this.id ^ (this.id >>> 32));
  }

  public boolean equals(Object obj) {
    if (obj instanceof AbstractIdentifier) {
      AbstractIdentifier other = (AbstractIdentifier) obj;
      return ((this.id == other.id) && this.getClass().equals(other.getClass()));
    }
    return false;
  }
}
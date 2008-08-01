/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.io.Serializable;

/**
 * Generic Identifier class, parent class of many ID types. Legal identifiers are expected to be >= 0 and -1 represents
 * a "null" identifier.
 * 
 * @author steve
 */
public abstract class AbstractIdentifier implements Comparable, Serializable {
  private static final long serialVersionUID = 1396710277826990138L;
  private static final long NULL_ID          = -1;
  private final long        id;

  /**
   * Create an identifier with a long value, which is expected to be >= 0.
   */
  public AbstractIdentifier(long id) {
    this.id = id;
  }

  /**
   * Create a null identifier
   */
  protected AbstractIdentifier() {
    this.id = NULL_ID;
  }

  /**
   * Check whether the identifier is null (-1).
   * 
   * @return True if -1, false otherwise
   */
  public boolean isNull() {
    return (this.id == NULL_ID);
  }

  /**
   * Convert to long
   * 
   * @return Long identifier value
   */
  public final long toLong() {
    return id;
  }

  public String toString() {
    return getIdentifierType() + "=" + "[" + id + "]";
  }

  /**
   * Subclasses of AbstractIdentifier specify their "type" by implementing this method and returning a string. The type
   * is used in printing toString().
   */
  abstract public String getIdentifierType();

  public int hashCode() {
    return (int) (this.id ^ (this.id >>> 32));
  }

  /**
   * Equality is based on the id value and the identifier class.
   */
  public boolean equals(Object obj) {
    if (obj instanceof AbstractIdentifier) {
      AbstractIdentifier other = (AbstractIdentifier) obj;
      return ((this.id == other.id) && this.getClass().equals(other.getClass()));
    }
    return false;
  }

  public int compareTo(Object o) {
    AbstractIdentifier other = (AbstractIdentifier) o;
    return (id < other.id ? -1 : (id == other.id ? 0 : 1));
  }
}

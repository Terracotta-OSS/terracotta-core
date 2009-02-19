/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.util.AbstractIdentifier;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

public class GarbageCollectionID extends AbstractIdentifier {

  private String                          uuid;

  /**
   * The NULL ObjectID
   */
  public final static GarbageCollectionID NULL_ID = new GarbageCollectionID();

  /**
   * Create a "null" GarbageCollectionID.
   */
  private GarbageCollectionID() {
    super();
  }

  /**
   * Create an GarbageCollectionID with the specified ID
   * 
   * @param id The id value, >= 0
   * @param UUID identifier
   */
  public GarbageCollectionID(long id, String uuid) {
    super(id);
    this.uuid = uuid;
  }

  public String getUUID() {
    return uuid;
  }

  public String getIdentifier() {
    return uuid + ":" + toLong();
  }

  @Override
  public String getIdentifierType() {
    return "GarbageCollectionID";
  }

  // TODO::Remove this and make gcIteration as long
  public int getIteration() {
    return (int) toLong();
  }

  public String toString() {
    return getIdentifierType() + "=" + "[" + getIdentifier() + "]";
  }

  @Override
  public int hashCode() {
    return getIdentifier().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof GarbageCollectionID) {
      GarbageCollectionID id = (GarbageCollectionID) obj;
      return getIdentifier().equals(id.getIdentifier());
    }
    return false;
  }

}

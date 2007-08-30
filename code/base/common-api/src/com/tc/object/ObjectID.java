/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

import java.io.Serializable;

/**
 * Object representing the ID of any managed object
 * 
 * @author steve
 */
public class ObjectID extends AbstractIdentifier implements Serializable {

  /**
   * The NULL ObjectID
   */
  public final static ObjectID NULL_ID = new ObjectID();

  /**
   * Create an ObjectID with the specified ID
   * @param id The id value, >= 0
   */
  public ObjectID(long id) {
    super(id);
  }

  /**
   * Create a "null" ObjectID.
   */
  private ObjectID() {
    super();
  }

  public String getIdentifierType() {
    return "ObjectID";
  }
}
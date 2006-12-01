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

  public final static ObjectID NULL_ID = new ObjectID();

  public ObjectID(long id) {
    super(id);
  }

  private ObjectID() {
    super();
  }

  public String getIdentifierType() {
    return "ObjectID";
  }
}
/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
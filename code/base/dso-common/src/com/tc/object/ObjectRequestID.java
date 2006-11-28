/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

public class ObjectRequestID extends AbstractIdentifier {
  
  public static final ObjectRequestID NULL_ID = new ObjectRequestID();

  private static final String ID_TYPE = "ObjectRequestID";

  public ObjectRequestID(long id) {
    super(id);
  }

  private ObjectRequestID() {
    super();
  }

  public String getIdentifierType() {
    return ID_TYPE;
  }

}
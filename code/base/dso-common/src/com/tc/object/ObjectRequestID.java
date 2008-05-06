/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
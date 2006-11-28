/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.session;

import com.tc.util.AbstractIdentifier;

public class SessionID extends AbstractIdentifier {

  public static final SessionID NULL_ID = new SessionID(-1);
  
  public SessionID(long id) {
    super(id);
  }

  public String getIdentifierType() {
    return "SessionID";
  }

}

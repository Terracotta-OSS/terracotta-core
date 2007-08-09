/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

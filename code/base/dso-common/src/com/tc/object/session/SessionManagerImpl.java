/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

import com.tc.util.sequence.Sequence;

public class SessionManagerImpl implements SessionManager, SessionProvider {

  private final Sequence sequence;
  private SessionID sessionID = SessionID.NULL_ID;
  
  public SessionManagerImpl(Sequence sequence) {
    this.sequence = sequence;
  }
    
  public synchronized SessionID getSessionID() {
    return sessionID;
  }

  public synchronized void newSession() {
    sessionID = new SessionID(sequence.next());
  }

  public synchronized boolean isCurrentSession(SessionID compare) {
    return sessionID.equals(compare);
  }

  public synchronized String toString() {
    return getClass().getName() + "[current session=" + sessionID + "]";
  }
  
}

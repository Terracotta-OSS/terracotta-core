/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

import com.tc.util.sequence.Sequence;

public class SessionManagerImpl implements SessionManager, SessionProvider {

  private final Sequence sequence;
  private SessionID sessionID = SessionID.NULL_ID;
  private SessionID nextSessionID = SessionID.NULL_ID;
  
  public SessionManagerImpl(Sequence sequence) {
    this.sequence = sequence;
  }
    
  public synchronized SessionID getSessionID() {
    return sessionID;
  }
  
  /*
   * Return the next session id will be when call newSession.
   * This advances session id but not apply to messages creation.
   * Message filter uses it to drop old messages when session changes.
   */
  public synchronized SessionID nextSessionID() {
    if (nextSessionID == SessionID.NULL_ID) {
      nextSessionID = new SessionID(sequence.next());
    }
    return (nextSessionID);
  }

  public synchronized void newSession() {
    if (nextSessionID != SessionID.NULL_ID) {
      sessionID = nextSessionID;
      nextSessionID = SessionID.NULL_ID;
    } else {
      sessionID = new SessionID(sequence.next());
    }
  }

  public synchronized boolean isCurrentSession(SessionID compare) {
    return sessionID.equals(compare);
  }

  public synchronized String toString() {
    return getClass().getName() + "[current session=" + sessionID + "]";
  }
  
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

import com.tc.util.sequence.Sequence;

public class SessionManagerImpl implements SessionManager {
  private Provider provider;
  private final SequenceFactory sequenceFactory;

  public SessionManagerImpl(SequenceFactory sequenceFactory) {
    this.sequenceFactory = sequenceFactory;
  }

  @Override
  public boolean isCurrentSession(SessionID sessionID) {
    Provider provider = getProvider();
    return provider.isCurrentSession(sessionID);
  }

  @Override
  public void newSession() {
    Provider provider = getProvider();
    provider.newSession();
  }

  @Override
  public SessionID getSessionID() {
    Provider provider = getProvider();
    return provider.getSessionID();
  }

  @Override
  public SessionID nextSessionID() {
    Provider provider = getProvider();
    return provider.nextSessionID();
  }

  @Override
  public synchronized void initProvider() {
    if (provider != null) { throw new RuntimeException("Session provider already exists"); }
    provider = new Provider(sequenceFactory.newSequence());
  }

  @Override
  public synchronized void resetSessionProvider() {
    provider = null;
  }

  private synchronized Provider getProvider() {
    return provider;
  }

  public interface SequenceFactory {
    public Sequence newSequence();
  }

  private static class Provider {
    private final Sequence sequence;
    private SessionID sessionID = SessionID.NULL_ID;
    private SessionID nextSessionID = SessionID.NULL_ID;

    public Provider(Sequence sequence) {
      this.sequence = sequence;
    }

    public synchronized SessionID getSessionID() {
      return sessionID;
    }

    /*
     * Return the next session id will be when call newSession. This advances session id but not apply to messages creation. Message filter
     * uses it to drop old messages when session changes.
     */
    public synchronized SessionID nextSessionID() {
      if (nextSessionID == SessionID.NULL_ID) {
        nextSessionID = new SessionID(sequence.next());
      }
      return nextSessionID;
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

    @Override
    public synchronized String toString() {
      return getClass().getName() + "[current session=" + sessionID + "]";
    }
  }

}

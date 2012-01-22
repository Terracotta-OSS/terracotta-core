/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

import com.tc.net.NodeID;
import com.tc.util.sequence.Sequence;

import java.util.HashMap;
import java.util.Map;

public class SessionManagerImpl implements SessionManager, SessionProvider {
  private final SequenceFactory       sequenceFactory;
  private final Map<NodeID, Provider> providersMap = new HashMap<NodeID, Provider>();

  public SessionManagerImpl(SequenceFactory sequenceFactory) {
    this.sequenceFactory = sequenceFactory;
  }

  public boolean isCurrentSession(NodeID nid, SessionID sessionID) {
    Provider provider = getProvider(nid);
    return provider.isCurrentSession(sessionID);
  }

  public void newSession(NodeID nid) {
    Provider provider = getProvider(nid);
    provider.newSession();
  }

  public SessionID getSessionID(NodeID nid) {
    Provider provider = getProvider(nid);
    return provider.getSessionID();
  }

  public SessionID nextSessionID(NodeID nid) {
    Provider provider = getProvider(nid);
    return provider.nextSessionID();
  }

  public void initProvider(NodeID nid) {
    synchronized (providersMap) {
      Provider provider = new Provider(sequenceFactory.newSequence());
      if (providersMap.put(nid, provider) != null) { throw new RuntimeException("Session provider already exists for "
                                                                                + nid); }
    }
  }

  public void resetSessionProvider() {
    synchronized (providersMap) {
      providersMap.clear();
    }
  }

  private Provider getProvider(NodeID nid) {
    synchronized (providersMap) {
      Provider provider = providersMap.get(nid);
      if (provider == null) { throw new RuntimeException("Session provider does not exist for " + nid); }
      return provider;
    }
  }

  public interface SequenceFactory {
    public Sequence newSequence();
  }

  private static class Provider {
    private final Sequence sequence;
    private SessionID      sessionID     = SessionID.NULL_ID;
    private SessionID      nextSessionID = SessionID.NULL_ID;

    public Provider(Sequence sequence) {
      this.sequence = sequence;
    }

    public synchronized SessionID getSessionID() {
      return sessionID;
    }

    /*
     * Return the next session id will be when call newSession. This advances session id but not apply to messages
     * creation. Message filter uses it to drop old messages when session changes.
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

    @Override
    public synchronized String toString() {
      return getClass().getName() + "[current session=" + sessionID + "]";
    }
  }

}

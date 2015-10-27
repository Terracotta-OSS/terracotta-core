/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

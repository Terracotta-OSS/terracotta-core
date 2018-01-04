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
package com.tc.voter;

public interface VoterManager {

  /**
   * Get registered with a server using the provided id.
   *
   * @param id voter id
   * @return the current term of the server
   */
  long registerVoter(String id);

  /**
   *
   * @param id voter id
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long heartbeat(String id);

  /**
   *
   * @param id the voter id
   * @param electionTerm the election term for which this vote is cast
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long vote(String id, long electionTerm);

  /**
   * For casting a veto vote during election.
   * A veto vote is accepted by the server if and only if the server is in the middle of an election.
   * Veto votes are ignored if the vote is cast when the server is not in election.
   *
   * @param id the voter id
   */
  boolean vetoVote(String id);


  /**
   *
   * Try and reconnect with the server after a disconnect.
   * If the reconnect attempt fails then the voter must try to re-register with the server.
   *
   * @param id voter id
   * @return the current term of the server if the reconnect succeeded. Else -1.
   */
  boolean reconnect(String id);

  /**
   * De-register the voter with the given id from the server.
   *
   * @param id the voter id
   * @return true if de-registration succeeds. Otherwise false.
   */
  boolean deregisterVoter(String id);
}

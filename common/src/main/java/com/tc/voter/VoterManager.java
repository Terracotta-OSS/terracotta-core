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

import java.util.concurrent.TimeoutException;

public interface VoterManager {

  long HEARTBEAT_RESPONSE = 0;
  long INVALID_VOTER_RESPONSE = -1;

  /**
   * Get registered with a server using the provided id.
   *
   * @param id voter id
   * @return the current term of the server. -1 if the registration fails.
   */
  long registerVoter(String id) throws TimeoutException;

  /**
   *
   * @param id voter id
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long heartbeat(String id) throws TimeoutException;

  /**
   *
   * @param id the voter id
   * @param electionTerm the election term for which this vote is cast
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long vote(String id, long electionTerm) throws TimeoutException;

  /**
   * For casting an override vote during election.
   * An override vote is accepted by the server if and only if the server is in the middle of an election.
   * Override votes are ignored if the vote is cast when the server is not in election.
   *
   * @param id the voter id
   */
  boolean overrideVote(String id) throws TimeoutException;

  /**
   * De-register the voter with the given id from the server.
   *
   * @param id the voter id
   * @return true if de-registration succeeds. Otherwise false.
   */
  boolean deregisterVoter(String id) throws TimeoutException;
}

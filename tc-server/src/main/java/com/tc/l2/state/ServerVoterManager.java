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
package com.tc.l2.state;

public interface ServerVoterManager {
  String MBEAN_NAME = "VoterManager";

  long HEARTBEAT_RESPONSE = 0;
  long INVALID_VOTER_RESPONSE = -1;
  /**
   * @param idTerm voter id and the election term number for which this vote is cast separated by a ":"
   * @return @see VoterManager
   */
  long vote(String idTerm);
  /**
   * Notify all voters to start voting the given election term.
   *
   * @param electionTerm current election term
   */
  void startVoting(long electionTerm, boolean cancelOverride);

  /**
   *
   * @return the total number of votes received so far
   */
  int getVoteCount();

  /**
   *
   * @return the configured limit of voters
   */
  int getVoterLimit();
  
    /**
   *
   * @return the total number of registered voters
   */
  int getRegisteredVoters();

  /**
   *
   * @return true if the server has received an override vote from some voter client. Else false.
   */
  boolean overrideVoteReceived();

  /**
   * Notify all voters to stop voting.
   */
  long stopVoting();

  /**
   * For casting an override vote during election.
   * An override vote is accepted by the server if and only if the server is in the middle of an election.
   * Override votes are ignored if the vote is cast when the server is not in election.
   *
   * @param id the voter id
   */
  boolean overrideVote(String id);


  /**
   * Get registered with a server using the provided id.
   *
   * @param id voter id
   * @return the current term of the server. -1 if the registration fails.
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
   * De-register the voter with the given id from the server.
   *
   * @param id the voter id
   * @return true if de-registration succeeds. Otherwise false.
   */
  boolean deregisterVoter(String id);
}

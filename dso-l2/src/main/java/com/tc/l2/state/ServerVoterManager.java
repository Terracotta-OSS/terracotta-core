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

import com.tc.voter.VoterManagerMBean;

public interface ServerVoterManager extends VoterManagerMBean {

  /**
   *
   * @return the configured limit of voters
   */
  int getVoterLimit();

  /**
   * Notify all voters to start voting the given election term.
   *
   * @param electionTerm current election term
   */
  void startVoting(long electionTerm);

  /**
   *
   * @return the total number of votes received so far
   */
  int getVoteCount();
  
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
}

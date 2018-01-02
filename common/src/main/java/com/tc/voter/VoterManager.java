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

  long HEARTBEAT_RESPONSE = 0;
  long INVALID_VOTER_RESPONSE = -1;
  long ERROR_RESPONSE = Long.MIN_VALUE;

  boolean registerVoter(String id);

  boolean confirmVoter(String id);

  /**
   *
   * @param id voter id
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long heartbeat(String id);

  /**
   *
   * @param idTerm A combination of the voter id and the election term for which this vote is cast separated by a :
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long vote(String idTerm);

  boolean reconnect(String id);

  boolean deregisterVoter(String id);
}

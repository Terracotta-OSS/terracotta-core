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

import com.tc.management.TerracottaMBean;
import com.tc.voter.VoterManager;

public interface ServerVoterManager extends VoterManager, TerracottaMBean {

  int getVoterLimit();

  void startElection(long electionTerm);

  int getVoteCount();

  void endElection();

  default long vote(String idTerm) {
    String[] split = idTerm.split(":");
    return vote(split[0], Long.parseLong(split[1]));
  }

  long vote(String id, long electionTerm);

}

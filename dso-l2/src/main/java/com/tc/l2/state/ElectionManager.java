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

import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.util.State;

public interface ElectionManager {

  public void declareWinner(Enrollment myNodeId, State currentState);

  public boolean handleStartElectionRequest(L2StateMessage msg, State currentState);

  public void handleElectionAbort(L2StateMessage msg, State currentState);

  public void handleElectionResultMessage(L2StateMessage msg, State currentState);

  public void reset(NodeID winnerNode, Enrollment winner);

  public long getElectionTime();
}

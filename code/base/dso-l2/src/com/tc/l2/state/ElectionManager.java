/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;

public interface ElectionManager {

  public NodeID runElection(NodeID myNodeId, boolean isNew, WeightGeneratorFactory weightsFactory);

  public void declareWinner(NodeID myNodeId);

  public boolean handleStartElectionRequest(L2StateMessage msg);

  public void handleElectionAbort(L2StateMessage msg);

  public void handleElectionResultMessage(L2StateMessage msg);

  public void reset(Enrollment winner);

  public long getElectionTime();
}

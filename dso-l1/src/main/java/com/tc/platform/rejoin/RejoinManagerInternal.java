/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

public interface RejoinManagerInternal extends RejoinManager {

  void init(DsoClusterInternalEventsGun dsoClusterEventsGun);

  void initiateRejoin(MessageChannel channel);

  void thisNodeJoinedCallback(ClientID oldNodeId, ClientID newNodeId);
}

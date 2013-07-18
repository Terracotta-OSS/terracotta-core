/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.net.ClientID;

public interface DsoClusterInternalEventsGun {

  void fireThisNodeJoined(ClientID nodeId, ClientID[] clusterMembers);

  void fireThisNodeLeft();

  void fireNodeJoined(ClientID nodeId);

  void fireNodeLeft(ClientID nodeId);

  void fireOperationsEnabled();

  void fireOperationsDisabled();

  void fireNodeError();


}

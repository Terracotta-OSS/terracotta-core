/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.clustermetadata;

import com.tc.object.msg.KeysForOrphanedValuesMessage;
import com.tc.object.msg.NodeMetaDataMessage;
import com.tc.object.msg.NodesWithKeysMessage;
import com.tc.object.msg.NodesWithObjectsMessage;

public interface ServerClusterMetaDataManager {
  public void handleMessage(NodesWithObjectsMessage message);
  public void handleMessage(KeysForOrphanedValuesMessage message);
  public void handleMessage(NodeMetaDataMessage message);
  public void handleMessage(NodesWithKeysMessage message);
}

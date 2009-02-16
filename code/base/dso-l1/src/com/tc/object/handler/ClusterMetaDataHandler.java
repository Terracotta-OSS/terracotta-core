/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.ClusterMetaDataManager;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessage;
import com.tc.object.msg.NodesWithObjectsResponseMessage;

public class ClusterMetaDataHandler extends AbstractEventHandler {

  private ClusterMetaDataManager clusterMetaDataManager;

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof NodesWithObjectsResponseMessage) {
      NodesWithObjectsResponseMessage message = (NodesWithObjectsResponseMessage)context;
      clusterMetaDataManager.setResponse(message.getThreadID(), message.getNodesWithObjects());
    } else if (context instanceof KeysForOrphanedValuesResponseMessage) {
      KeysForOrphanedValuesResponseMessage message = (KeysForOrphanedValuesResponseMessage)context;
      clusterMetaDataManager.setResponse(message.getThreadID(), message.getKeys());
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.clusterMetaDataManager = ccc.getClusterMetaDataManager();
  }

}

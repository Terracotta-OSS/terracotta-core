/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.cluster.ClusterEvent;
import com.tc.cluster.ClusterListener;
import com.tcclient.cluster.ClusterInternal.ClusterEventType;

public interface OutOfBandClusterListener extends ClusterListener {

  boolean useOutOfBandNotification(ClusterEventType type, ClusterEvent event);

}

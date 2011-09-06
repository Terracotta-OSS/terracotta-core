/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tcclient.cluster.DsoClusterInternal.DsoClusterEventType;

public interface OutOfBandDsoClusterListener extends DsoClusterListener {

  boolean useOutOfBandNotification(DsoClusterEventType type, DsoClusterEvent event);

}

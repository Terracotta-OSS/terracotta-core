/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.net.NodeID;

public interface LockStatisticsReponseMessageFactory {
  LockStatisticsResponseMessage newLockStatisticsResponseMessage(final NodeID remoteID);
}

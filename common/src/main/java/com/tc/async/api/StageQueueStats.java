/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.stats.Stats;

public interface StageQueueStats extends Stats {

  String getName();

  int getDepth();
}

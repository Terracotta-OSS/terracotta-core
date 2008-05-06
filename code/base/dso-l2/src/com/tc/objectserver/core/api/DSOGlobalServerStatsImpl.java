/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.impl.ObjectManagerStatsImpl;
import com.tc.stats.counter.sampled.SampledCounter;

public class DSOGlobalServerStatsImpl implements DSOGlobalServerStats {

  private final SampledCounter faultCounter;
  private final SampledCounter flushCounter;
  private final SampledCounter txnCounter;
  private final ObjectManagerStatsImpl objMgrStats;

  private final SampledCounter broadcastCounter;
  private final SampledCounter changesCounter;

  public DSOGlobalServerStatsImpl(SampledCounter flushCounter, SampledCounter faultCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter changesCounter) {
    this.flushCounter = flushCounter;
    this.faultCounter = faultCounter;
    this.txnCounter = txnCounter;
    this.objMgrStats = objMgrStats;
    this.broadcastCounter = broadcastCounter;
    this.changesCounter = changesCounter;
  }

  public SampledCounter getObjectFlushCounter() {
    return this.flushCounter;
  }

  public SampledCounter getObjectFaultCounter() {
    return this.faultCounter;
  }

  public ObjectManagerStats getObjectManagerStats() {
    return this.objMgrStats;
  }

  public SampledCounter getTransactionCounter() {
    return this.txnCounter;
  }

  public SampledCounter getBroadcastCounter() {
    return broadcastCounter;
  }

  public SampledCounter getChangesCounter() {
    return changesCounter;
  }
}

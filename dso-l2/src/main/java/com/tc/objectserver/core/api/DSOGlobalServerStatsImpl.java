/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.impl.ObjectManagerStatsImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;

public class DSOGlobalServerStatsImpl implements DSOGlobalServerStats {

  private final SampledCounter           faultCounter;
  private final SampledCounter           flushCounter;
  private final SampledCounter           txnCounter;
  private final ObjectManagerStatsImpl   objMgrStats;

  private final SampledCounter           broadcastCounter;
  private final SampledCounter           l2FaultFromDiskCounter;
  private final SampledCounter           time2FaultFromDisk;
  private final SampledCounter           time2Add2ObjMgr;
  private final SampledCounter           globalLockCounter;
  private final SampledCounter           globalLockRecallCounter;
  private final SampledRateCounter       changesPerBroadcast;
  private final SampledRateCounter       transactionSizeCounter;
  private SampledCumulativeCounter serverMapGetSizeRequestsCounter;
  private SampledCumulativeCounter serverMapGetValueRequestsCounter;
  private SampledCumulativeCounter serverMapGetSnapshotRequestsCounter;

  public DSOGlobalServerStatsImpl(SampledCounter flushCounter, SampledCounter faultCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter l2FaultFromDiskCounter, SampledCounter time2FaultFromDisk,
                                  SampledCounter time2Add2ObjMgr, SampledCounter globalLockRecallCounter,
                                  SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                                  SampledCounter globalLockCounter) {
    this.flushCounter = flushCounter;
    this.faultCounter = faultCounter;
    this.txnCounter = txnCounter;
    this.objMgrStats = objMgrStats;
    this.broadcastCounter = broadcastCounter;
    this.l2FaultFromDiskCounter = l2FaultFromDiskCounter;
    this.time2FaultFromDisk = time2FaultFromDisk;
    this.time2Add2ObjMgr = time2Add2ObjMgr;
    this.globalLockRecallCounter = globalLockRecallCounter;
    this.changesPerBroadcast = changesPerBroadcast;
    this.transactionSizeCounter = transactionSizeCounter;
    this.globalLockCounter = globalLockCounter;
  }
  
  public DSOGlobalServerStatsImpl serverMapGetSizeRequestsCounter(final SampledCumulativeCounter counter) {
    this.serverMapGetSizeRequestsCounter = counter;
    return this;
  }
  
  public DSOGlobalServerStatsImpl serverMapGetValueRequestsCounter(final SampledCumulativeCounter counter) {
    this.serverMapGetValueRequestsCounter = counter;
    return this;
  }
  public DSOGlobalServerStatsImpl serverMapGetSnapshotRequestsCounter(final SampledCumulativeCounter counter) {
    this.serverMapGetSnapshotRequestsCounter = counter;
    return this;
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

  public SampledCounter getL2FaultFromDiskCounter() {
    return l2FaultFromDiskCounter;
  }

  public SampledCounter getTime2FaultFromDisk() {
    return time2FaultFromDisk;
  }

  public SampledCounter getTime2Add2ObjectMgr() {
    return time2Add2ObjMgr;
  }

  public SampledCounter getGlobalLockRecallCounter() {
    return globalLockRecallCounter;
  }

  public SampledRateCounter getChangesPerBroadcastCounter() {
    return changesPerBroadcast;
  }

  public SampledRateCounter getTransactionSizeCounter() {
    return transactionSizeCounter;
  }

  public SampledCounter getGlobalLockCounter() {
    return this.globalLockCounter;
  }

  public SampledCumulativeCounter getServerMapGetSizeRequestsCounter() {
    return serverMapGetSizeRequestsCounter;
  }

  public SampledCumulativeCounter getServerMapGetValueRequestsCounter() {
    return serverMapGetValueRequestsCounter;
  }
  
  public SampledCumulativeCounter getServerMapGetSnapshotRequestsCounter() {
    return serverMapGetSnapshotRequestsCounter;
  }

}

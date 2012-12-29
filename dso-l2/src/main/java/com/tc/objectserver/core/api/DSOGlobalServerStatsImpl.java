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
  private final SampledCounter       evictionRateCounter;
  private final SampledCounter       expirationRateCounter;

  private final SampledCounter           broadcastCounter;
  private final SampledCounter           globalLockCounter;
  private final SampledCounter           globalLockRecallCounter;
  private final SampledRateCounter       changesPerBroadcast;
  private final SampledRateCounter       transactionSizeCounter;
  private SampledCounter                 operationCounter;

  private SampledCumulativeCounter serverMapGetSizeRequestsCounter;
  private SampledCumulativeCounter serverMapGetValueRequestsCounter;
  private SampledCumulativeCounter serverMapGetSnapshotRequestsCounter;

  public DSOGlobalServerStatsImpl(SampledCounter flushCounter, SampledCounter faultCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter globalLockRecallCounter,
                                  SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                                  SampledCounter globalLockCounter, SampledCounter evictionRateCounter,
                                  SampledCounter expirationRateCounter) {
    this.flushCounter = flushCounter;
    this.faultCounter = faultCounter;
    this.txnCounter = txnCounter;
    this.objMgrStats = objMgrStats;
    this.evictionRateCounter = evictionRateCounter;
    this.expirationRateCounter = expirationRateCounter;
    this.broadcastCounter = broadcastCounter;
    this.globalLockRecallCounter = globalLockRecallCounter;
    this.changesPerBroadcast = changesPerBroadcast;
    this.transactionSizeCounter = transactionSizeCounter;
    this.globalLockCounter = globalLockCounter;
  }
  
  public DSOGlobalServerStatsImpl(SampledCounter flushCounter, SampledCounter faultCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter globalLockRecallCounter,
                                  SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                                  SampledCounter globalLockCounter, SampledCounter evictionRateCounter,
                                  SampledCounter expirationRateCounter, SampledCounter operationCounter) {
    this(flushCounter, faultCounter, txnCounter, objMgrStats, broadcastCounter, globalLockRecallCounter, changesPerBroadcast,
        transactionSizeCounter, globalLockCounter, evictionRateCounter, expirationRateCounter);
    this.operationCounter = operationCounter;
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

  @Override
  public SampledCounter getOperationCounter() {
    return operationCounter;
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

  @Override
  public SampledCounter getEvictionRateCounter() {
    return evictionRateCounter;
  }

  @Override
  public SampledCounter getExpirationRateCounter() {
    return expirationRateCounter;
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;

public class GlobalServerStatsImpl implements GlobalServerStats {

  private final SampledCounter readCounter;
  private final SampledCounter           txnCounter;

  private final SampledCounter           broadcastCounter;
  private final SampledCounter           globalLockCounter;
  private final SampledCounter           globalLockRecallCounter;
  private final SampledRateCounter       changesPerBroadcast;
  private final SampledRateCounter       transactionSizeCounter;
  private SampledCounter                 operationCounter;

  private SampledCumulativeCounter serverMapGetSizeRequestsCounter;
  private SampledCumulativeCounter serverMapGetValueRequestsCounter;
  private SampledCumulativeCounter serverMapGetSnapshotRequestsCounter;

  public GlobalServerStatsImpl(SampledCounter readCounter, SampledCounter txnCounter,
                               SampledCounter broadcastCounter,
                               SampledCounter globalLockRecallCounter,
                               SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                               SampledCounter globalLockCounter) {
    this.readCounter = readCounter;
    this.txnCounter = txnCounter;
    this.broadcastCounter = broadcastCounter;
    this.globalLockRecallCounter = globalLockRecallCounter;
    this.changesPerBroadcast = changesPerBroadcast;
    this.transactionSizeCounter = transactionSizeCounter;
    this.globalLockCounter = globalLockCounter;
  }
  
  public GlobalServerStatsImpl(SampledCounter readCounter, SampledCounter txnCounter,
                               SampledCounter broadcastCounter,
                               SampledCounter globalLockRecallCounter,
                               SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                               SampledCounter globalLockCounter,
                               SampledCounter operationCounter) {
    this(readCounter, txnCounter, broadcastCounter, globalLockRecallCounter, changesPerBroadcast,
        transactionSizeCounter, globalLockCounter);
    this.operationCounter = operationCounter;
  }

  public GlobalServerStatsImpl serverMapGetSizeRequestsCounter(SampledCumulativeCounter counter) {
    this.serverMapGetSizeRequestsCounter = counter;
    return this;
  }
  
  public GlobalServerStatsImpl serverMapGetValueRequestsCounter(SampledCumulativeCounter counter) {
    this.serverMapGetValueRequestsCounter = counter;
    return this;
  }
  public GlobalServerStatsImpl serverMapGetSnapshotRequestsCounter(SampledCumulativeCounter counter) {
    this.serverMapGetSnapshotRequestsCounter = counter;
    return this;
  }

  @Override
  public SampledCounter getReadOperationRateCounter() {
    return this.readCounter;
  }

  @Override
  public SampledCounter getTransactionCounter() {
    return this.txnCounter;
  }

  @Override
  public SampledCounter getBroadcastCounter() {
    return broadcastCounter;
  }

  @Override
  public SampledCounter getGlobalLockRecallCounter() {
    return globalLockRecallCounter;
  }

  @Override
  public SampledRateCounter getChangesPerBroadcastCounter() {
    return changesPerBroadcast;
  }

  @Override
  public SampledRateCounter getTransactionSizeCounter() {
    return transactionSizeCounter;
  }

  @Override
  public SampledCounter getGlobalLockCounter() {
    return this.globalLockCounter;
  }

  @Override
  public SampledCounter getOperationCounter() {
    return operationCounter;
  }

  @Override
  public SampledCumulativeCounter getServerMapGetSizeRequestsCounter() {
    return serverMapGetSizeRequestsCounter;
  }

  @Override
  public SampledCumulativeCounter getServerMapGetValueRequestsCounter() {
    return serverMapGetValueRequestsCounter;
  }
  
  public SampledCumulativeCounter getServerMapGetSnapshotRequestsCounter() {
    return serverMapGetSnapshotRequestsCounter;
  }

}

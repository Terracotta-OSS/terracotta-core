/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.core.api;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;

public class GlobalServerStatsImpl implements GlobalServerStats {

  private final SampledCounter readCounter;
  private final SampledCounter           txnCounter;

  private final SampledCounter           broadcastCounter;
  private final SampledRateCounter       changesPerBroadcast;
  private final SampledRateCounter       transactionSizeCounter;
  private SampledCounter                 operationCounter;

  private SampledCumulativeCounter serverMapGetSizeRequestsCounter;
  private SampledCumulativeCounter serverMapGetValueRequestsCounter;
  private SampledCumulativeCounter serverMapGetSnapshotRequestsCounter;

  public GlobalServerStatsImpl(SampledCounter readCounter, SampledCounter txnCounter,
                               SampledCounter broadcastCounter,
                               SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter) {
    this.readCounter = readCounter;
    this.txnCounter = txnCounter;
    this.broadcastCounter = broadcastCounter;
    this.changesPerBroadcast = changesPerBroadcast;
    this.transactionSizeCounter = transactionSizeCounter;
  }
  
  public GlobalServerStatsImpl(SampledCounter readCounter, SampledCounter txnCounter,
                               SampledCounter broadcastCounter,
                               SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                               SampledCounter operationCounter) {
    this(readCounter, txnCounter, broadcastCounter, changesPerBroadcast,
        transactionSizeCounter);
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
  public SampledRateCounter getChangesPerBroadcastCounter() {
    return changesPerBroadcast;
  }

  @Override
  public SampledRateCounter getTransactionSizeCounter() {
    return transactionSizeCounter;
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

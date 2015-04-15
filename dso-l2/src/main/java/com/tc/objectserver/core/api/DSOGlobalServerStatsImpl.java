/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.impl.ObjectManagerStatsImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;

public class DSOGlobalServerStatsImpl implements DSOGlobalServerStats {

  private final SampledCounter readCounter;
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

  public DSOGlobalServerStatsImpl(SampledCounter readCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter globalLockRecallCounter,
                                  SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                                  SampledCounter globalLockCounter, SampledCounter evictionRateCounter,
                                  SampledCounter expirationRateCounter) {
    this.readCounter = readCounter;
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
  
  public DSOGlobalServerStatsImpl(SampledCounter readCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter globalLockRecallCounter,
                                  SampledRateCounter changesPerBroadcast, SampledRateCounter transactionSizeCounter,
                                  SampledCounter globalLockCounter, SampledCounter evictionRateCounter,
                                  SampledCounter expirationRateCounter, SampledCounter operationCounter) {
    this(readCounter, txnCounter, objMgrStats, broadcastCounter, globalLockRecallCounter, changesPerBroadcast,
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

  @Override
  public SampledCounter getReadOperationRateCounter() {
    return this.readCounter;
  }

  @Override
  public ObjectManagerStats getObjectManagerStats() {
    return this.objMgrStats;
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

  @Override
  public SampledCounter getEvictionRateCounter() {
    return evictionRateCounter;
  }

  @Override
  public SampledCounter getExpirationRateCounter() {
    return expirationRateCounter;
  }
}

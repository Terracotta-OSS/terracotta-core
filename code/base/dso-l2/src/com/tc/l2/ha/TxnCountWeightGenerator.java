/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.Assert;

public class TxnCountWeightGenerator implements WeightGenerator {
  private final ServerTransactionManager serverTransactionManager;

  public TxnCountWeightGenerator(ServerTransactionManager serverTransactionManager) {
    Assert.assertNotNull(serverTransactionManager);
    this.serverTransactionManager = serverTransactionManager;
  }

  public long getWeight() {
    return serverTransactionManager.getTotalNumOfActiveTransactions();
  }

}

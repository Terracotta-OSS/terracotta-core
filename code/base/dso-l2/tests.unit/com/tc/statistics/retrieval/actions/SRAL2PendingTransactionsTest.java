/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.Random;

public class SRAL2PendingTransactionsTest extends TCTestCase {

  private ServerTransactionManager serverTransactionManager;

  protected void setUp() throws Exception {
    serverTransactionManager = new TestServerTransactionManager() {
      Random random = new Random(System.currentTimeMillis());

      public int getTotalPendingTransactionsCount() {
        return random.nextInt(20) + 1;
      }
    };
  }

  public void testL2PendingTransactionsSRA() {
    SRAL2PendingTransactions pendingTransactions = new SRAL2PendingTransactions(serverTransactionManager);
    Assert.assertEquals(StatisticType.SNAPSHOT, pendingTransactions.getType());
    for (int i = 0; i < 30; i++) {
      System.out.println("Getting pending transactions...");
      StatisticData[] data = pendingTransactions.retrieveStatisticData();
      Assert.assertEquals(1, data.length);
      assertData(data[0], 1, 20);
    }
  }

  private void assertData(final StatisticData statisticData, final int min, final int max) {
    Assert.assertEquals(SRAL2PendingTransactions.ACTION_NAME, statisticData.getName());
    Assert.assertNull(statisticData.getAgentIp());
    Assert.assertNull(statisticData.getAgentDifferentiator());
    Long count = (Long)statisticData.getData();
    Assert.eval(count >= min && count <= max);
    System.out.println("Asserted statistic data.");
  }

  protected void tearDown() throws Exception {
    serverTransactionManager = null;
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.util.Assert;

import junit.framework.TestCase;

public class SRADistributedGCTest extends TestCase {

  public void testDistributedGCSRA() {
    SRADistributedGC sra = new SRADistributedGC();
    Assert.assertEquals(sra.getName(), SRADistributedGC.ACTION_NAME);
    Assert.assertEquals(sra.getType(), StatisticType.TRIGGERED);

    Assert.assertSame(StatisticRetrievalAction.EMPTY_STATISTIC_DATA, sra.retrieveStatisticData());
  }
}

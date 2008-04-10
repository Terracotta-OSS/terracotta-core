/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

public class SRACacheObjectsEvictedTest extends TCTestCase {

  public void testCacheObjectsEvicted() {
    SRACacheObjectsEvicted sra = new SRACacheObjectsEvicted();
    Assert.assertEquals(sra.getName(), SRACacheObjectsEvicted.ACTION_NAME);
    Assert.assertEquals(sra.getType(), StatisticType.TRIGGERED);

    Assert.assertSame(StatisticRetrievalAction.EMPTY_STATISTIC_DATA, sra.retrieveStatisticData());
  }
}

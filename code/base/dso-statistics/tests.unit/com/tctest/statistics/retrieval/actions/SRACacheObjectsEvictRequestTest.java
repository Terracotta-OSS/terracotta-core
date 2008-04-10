/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

public class SRACacheObjectsEvictRequestTest extends TCTestCase {

  public void testCacheObjectsEvictRequest() {
    SRACacheObjectsEvictRequest sra = new SRACacheObjectsEvictRequest();
    Assert.assertEquals(sra.getName(), SRACacheObjectsEvictRequest.ACTION_NAME);
    Assert.assertEquals(sra.getType(), StatisticType.TRIGGERED);

    Assert.assertSame(StatisticRetrievalAction.EMPTY_STATISTIC_DATA, sra.retrieveStatisticData());
  }
}
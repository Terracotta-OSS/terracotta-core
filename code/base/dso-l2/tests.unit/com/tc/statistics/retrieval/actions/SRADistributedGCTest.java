/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import junit.framework.TestCase;

import com.tc.util.Assert;
import com.tc.statistics.StatisticType;

public class SRADistributedGCTest extends TestCase {

  public void testDistributedGCSRA() {
    SRADistributedGC sra = new SRADistributedGC();
    Assert.assertEquals(sra.getName(), SRADistributedGC.ACTION_NAME);
    Assert.assertEquals(sra.getType(), StatisticType.TRIGGERED);

    try {
      sra.retrieveStatisticData();
      fail("SRADistributedGC cannot be used to collect statistics data");
    } catch (UnsupportedOperationException e) {
      //ok
    }
  }
}

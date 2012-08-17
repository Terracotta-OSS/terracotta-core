/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Vm;

import java.util.Map;

import junit.framework.Assert;

public class HashMapBatchTxnTestClient extends ClientBase {
  int BATCHSIZE = (Vm.isIBM()) ? 400 : 1000;
  int BATCHES   = 40;

  public HashMapBatchTxnTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    int index = getBarrierForAllClients().await();
    Map sharedMap = toolkit.getMap("testMap", null, null);
    for (int batch = 0; batch < BATCHES; batch += 2) {
      if (index == 0) {
        System.out.println("XXX Batching(client=0) " + batch);
        int id = BATCHSIZE * batch;
        for (int i = 0; i < BATCHSIZE; ++i) {
          sharedMap.put(Integer.valueOf(id), generateValueForId(id));
          ++id;
        }
      }
      if (index == 1) {
        System.out.println("XXX Batching(client=1) " + (batch + 1));
        int id = BATCHSIZE * batch + BATCHSIZE;
        for (int i = 0; i < BATCHSIZE; ++i) {
          sharedMap.put(Integer.valueOf(id), generateValueForId(id));
          ++id;
        }
      }
      ThreadUtil.reallySleep(20);
    }

    getBarrierForAllClients().await();

    /* verification */
    System.out.println("XXX starting verification");
    for (int batch = 0; batch < BATCHES; ++batch) {
      System.out.println("XXX verifying batch " + batch);
      for (int i = 0; i < BATCHSIZE; ++i) {
        String value = (String) sharedMap.get(Integer.valueOf(batch * BATCHSIZE + i));
        Assert.assertEquals(generateValueForId(batch * BATCHSIZE + i), value);
      }
      ThreadUtil.reallySleep(20);
    }
    System.out.println("XXX verification done");

  }

  private String generateValueForId(int id) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 100; i++) {
      sb.append("Value-" + id);
    }
    return sb.toString();
  }

}

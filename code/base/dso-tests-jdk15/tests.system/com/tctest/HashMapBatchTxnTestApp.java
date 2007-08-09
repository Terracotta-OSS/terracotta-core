/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;

public class HashMapBatchTxnTestApp extends AbstractTransparentApp {
  int BATCHSIZE = 2000;
  int BATCHES = 200;
  
  private final CyclicBarrier     barrier; 
  private final HashMap<Integer,  HashMap> hashmap_root = new HashMap<Integer, HashMap>();

  public HashMapBatchTxnTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      testHashMapBatchTxn();     
    } catch (Throwable t) {
      notifyError(t);
    }
  }
 
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = HashMapBatchTxnTestApp.class.getName();
    ITransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");    
    spec.addRoot("hashmap_root", "hashmap_root");
  }
  
  private void testHashMapBatchTxn() throws Exception {
    int index = barrier.await();
    
    for(int batch = 0; batch < BATCHES; ++batch) {
      if(index == 0) {
        synchronized(hashmap_root) {
          System.out.println("XXX Batching(client=0) "+batch);
          int id = BATCHSIZE * batch;
          for(int i = 0; i < BATCHSIZE; ++i) {
            HashMap<Integer,Integer> submap = generateNewHashMap(id, 10+(id%10));
            hashmap_root.put(new Integer(id), submap);
            ++id;
          }
        }    
      }
      if(index == 1) {
        synchronized(hashmap_root) {
          System.out.println("XXX Batching(client=1) "+batch);
          int id = BATCHSIZE * batch + BATCHSIZE;
          for(int i = 0; i < BATCHSIZE; ++i) {
            HashMap<Integer,Integer> submap = generateNewHashMap(id, 10+(id%10));
            hashmap_root.put(new Integer(id), submap);
            ++id;
          }
        }    
      }
    }
    
    barrier.await();
    
    /* verification */
    System.out.println("XXX starting verification");
    for (int batch = 0; batch < BATCHES; ++batch) {
      System.out.println("XXX verifying batch "+batch);
      for (int i = 0; i < (BATCHSIZE*2); ++i) {
        HashMap<Integer,Integer> submap = hashmap_root.get(new Integer(i));
        Assert.assertTrue("Sub-HashMap("+i+") size is "+submap.size()+ " but expect "+(10+(i%10)), submap.size() == (10+(i%10))); 
      }
    }
    System.out.println("XXX verification done");
    
  }
  
  HashMap generateNewHashMap(int startIndex, int size) {
    HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
    
    for(int i = startIndex; i < (startIndex+size); ++i) {
      map.put(new Integer(i), new Integer(i));
    }
    
    return(map);
  }

}

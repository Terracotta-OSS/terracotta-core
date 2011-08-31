/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.LinkedList;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentLinkedListTestApp extends AbstractTransparentApp {

  
  private final CyclicBarrier     barrier; 
  private final LinkedList<Integer> sharedList = new LinkedList<Integer>();
  private final LinkedList<Integer> resultList = new LinkedList<Integer>();

  public ConcurrentLinkedListTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

     testProducerConsumer(index);
      
    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  
  /**
   * Test producer consumer on a shared list
   */
  private void testProducerConsumer(int index) throws Exception {
    int upbound = 1000;
    
    // thread 0 as a produce
    if(index == 0) {
      int val = 0;
      synchronized(sharedList) {
        resultList.clear();
        sharedList.clear();
      }
      
      while(val <= upbound){
        synchronized(sharedList) {         
          sharedList.add(val);
          // System.out.println("*** Produces item " + val);
          ++val;
          sharedList.notifyAll();
        }
        Thread.sleep(1 + (int)(Math.random() * 2));
      }
    }
    
    if(index != 0) {
      int val = 0;
      while(val < upbound){
        synchronized(sharedList) {
          while(sharedList.isEmpty()) {
            sharedList.wait();
          }
          val = sharedList.removeFirst();
          resultList.add(val);
          // System.out.println("*** Thread " + index + " consumes item " + val);
          // put the last item back as an end indicator
          if(val == upbound) {
            sharedList.add(val);
            sharedList.notifyAll();
          }
        }
        Thread.sleep(5 + (int)(Math.random() * 5));
      }
    }
    
    barrier.await();
    
    // verify
    for(int i = 0; i < upbound; ++i) {
      // it is right to use the same lock
      synchronized(sharedList) {
        // System.out.println("*** Verify["+i+"] value="+resultList.get(i));
        Assert.assertEquals("Client " + ManagerUtil.getClientID() + " : Result Element " + i, i, resultList.get(i).intValue());
      }
    }
    
    barrier.await();
  }
  

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ConcurrentLinkedListTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("sharedList", "sharedList");
    spec.addRoot("resultList", "resultList");
  }
}

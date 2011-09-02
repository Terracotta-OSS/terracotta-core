/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.GCTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Test case for DEV-1283
 * To cause OOME, the following constants to said values:
 *   THREADS_PER_NODE_COUNT = 20
 *   NODE_COUNT = 5
 *   NUM_OF_OBJ_PER_THREAD = 10000
 */
public class HashMapValueSharedTest extends GCTestBase {
  
  
  private static final int THREADS_PER_NODE_COUNT = 8;
  private static final int NODE_COUNT = 2;

  protected Class getApplicationClass() {
    return HashMapValuedShareApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.getTransparentAppConfig().setApplicationInstancePerClientCount(THREADS_PER_NODE_COUNT);
    t.initializeTestRunner();
  }

  public static class HashMapValuedShareApp extends AbstractTransparentApp {
    

    private static final int    NUM_OF_OBJ_PER_THREAD = 1000;
    private final HashMap       hashmapRoot = new HashMap();
    private final CyclicBarrier barrier;

    public HashMapValuedShareApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

      String testClass = HashMapValuedShareApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      config.addIncludePattern(testClass + "$*", false, false, true);

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("hashmapRoot", "hashmapRoot");
      spec.addRoot("barrier", "barrier");
    }

    private static String key(String valueIdent) {
      return String.valueOf(Math.random() % 100) + valueIdent;
    }

    public void run() {

      // barrier for shared value object
      try {
        barrier.await();

      } catch (InterruptedException e) {
        throw new AssertionError(e);
      } catch (BrokenBarrierException e) {
        throw new AssertionError(e);
      }

      // populate with shared objects
      for (int i = 0; i < NUM_OF_OBJ_PER_THREAD; i++) {

        ValueObject vo = null;
        synchronized (hashmapRoot) {
          String key = key("Node1");
          vo = (ValueObject) hashmapRoot.get(key);
          if (vo == null) {
            vo = new ValueObject(new Object());
            hashmapRoot.put(key, vo);
          } else {
            //stats.incrementRead();
          }
        }
        
        synchronized (hashmapRoot) {
          String key = key("Node2");
          ValueObject vo2 = (ValueObject) hashmapRoot.get(key);
          if (vo2 == null) {
            vo2 = new ValueObject(vo.getObj());
            hashmapRoot.put(key, vo2);
           } else {
            vo2.setObj( vo.getObj() );
          }
        }
        
        
      }

    }

    private static class ValueObject {

      public ValueObject(Object obj) {
        this.obj = obj;
      }

      private Object obj;
      
      public Object getObj() {
        return obj;
      }
      
      public void setObj(Object obj) {
        this.obj = obj;
      }

    }
    
   

  }

}

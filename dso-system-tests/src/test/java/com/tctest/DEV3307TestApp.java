/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.ConcurrentHashMap;

public class DEV3307TestApp extends AbstractTransparentApp {
  
  private static ConcurrentHashMap<PayLoad, String> data = new ConcurrentHashMap<PayLoad, String>();

  public DEV3307TestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }
  
  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = DEV3307TestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    new SynchronizedIntSpec().visit(visitor, config);

    spec.addRoot("data", "data");
  }

  public void run() {
    Thread [] runner =  new Thread[16];
    for(int i = 0; i < 16; i++) {
      runner[i] = new Thread(new Runner(), i+ "");
      runner[i].start();
    }
    for(int i =0; i < 16; i++){
      try {
        runner[i].join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

  }
  
  private static class Runner implements Runnable {

    public void run() {
      int threadId = Integer.parseInt(Thread.currentThread().getName());
      for (int j = 0; j < 10000; j++) {
        data.put(new PayLoad("01234567890123456789012345678901234567890123456789" + (1000000 * threadId + j)),
                 (1000000 * threadId + j) + "");
      }
    }

  }
  static class PayLoad {
    String payLoad;
    PayLoad(String payLoad) {
      this.payLoad = payLoad;
    }
    
//    public int hashCode() {
//      return payLoad.hashCode();
//    }

  }
}

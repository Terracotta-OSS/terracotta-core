/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;

/**
 * Test that calling intern() on a String which is still compressed in L1 will
 * decompress String before interning it
 */
public class InternCompressedStringTest extends TransparentTestBase {

  private static final int NODE_COUNT    = 2;
  
  protected Class getApplicationClass() {
    return InternCompressedStringTestApp.class;
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  public static class InternCompressedStringTestApp extends AbstractErrorCatchingTransparentApp {
    
    private final List root;
    private final CyclicBarrier barrier;

    public InternCompressedStringTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      root = new ArrayList();
      barrier = new CyclicBarrier(getParticipantCount());
    }
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      final String testClass = InternCompressedStringTestApp.class.getName();
      final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
      String methodExpression = "* " + testClass + ".read(..)";
      config.addReadAutolock(methodExpression);
      methodExpression = "* " + testClass + ".put(..)";
      config.addWriteAutolock(methodExpression);
      new CyclicBarrierSpec().visit(visitor, config);
    }
    
    protected void runTest() throws Throwable {
      // TODO assert string compression size property
      
      // phase 1 - one node loads large String
      final int index = barrier.barrier();
      final String expected = getTestString();
      if (index == 0) {
        put(expected, root);
      }
      
      // phase 2 intern - different node retrieves compressed String, interns it
      barrier.barrier();
      if (index > 0) {
        final String actual = read(0, root).intern();
        assertEquals(expected, actual);
      }
    }
    
    /* needs to be big enough that it is compressed */
    private String getTestString(){
      final StringBuffer sb = new StringBuffer("f");
      for (int i=0; i<512; i++){
        sb.append("o");
      }
      sb.append("!");
      return sb.toString();
    }
    
    private void put(String value, List list){
      synchronized(list){
        list.add(value);
      }
    }
    
    private String read(int index, List list) {
      synchronized (list) {
        return (String) list.get(index);
      }
    }    
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StringArrayCopyMethodsTestApp extends AbstractErrorCatchingTransparentApp {

  private final Map           map = new HashMap();
  private final CyclicBarrier barrier;

  public StringArrayCopyMethodsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {

    synchronized (map) {
      if (map.isEmpty()) {
        map.put("byteArray", new byte[5]);
        map.put("charArray", new char[5]);
      }
    }

    char[] ca = (char[]) map.get("charArray");
    byte[] ba = (byte[]) map.get("byteArray");

    int num = barrier.barrier();
    if (num == 0) {
      synchronized (ba) {
        // copy into the managed byte array
        "Hi Tim, you are a golden god".getBytes(3, 6, ba, 1);
      }

      synchronized (ca) {
        // copy into the managed char array
        "Gotta head to Santa Cruz for a haircut".getChars(6, 10, ca, 1);
      }
    }

    barrier.barrier();

    Assert.assertTrue(Arrays.equals(new byte[] { 0, (byte) 'T', (byte) 'i', (byte) 'm', 0 }, ba));
    Assert.assertTrue(Arrays.equals(new char[] { 0, 'h', 'e', 'a', 'd' }, ca));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, com.tc.object.config.DSOClientConfigHelper config) {
    String testClass = StringArrayCopyMethodsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("map", "map");

    new CyclicBarrierSpec().visit(visitor, config);
  }

}

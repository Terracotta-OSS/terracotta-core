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
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class RandomIOClassesTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  
  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return RandomIOClassesTestApp.class;
  }

  public static class RandomIOClassesTestApp extends AbstractErrorCatchingTransparentApp {

    private final Map           map = new HashMap();
    private final CyclicBarrier barrier;

    public RandomIOClassesTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      this.barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {
      int index = barrier.barrier();

      if (index == 0) {
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        synchronized (map) {
          map.put("sw", sw);
          map.put("bw", new BufferedWriter(sw));
          map.put("baos", baos);
          map.put("dos", new DataOutputStream(baos));
        }
      }

      barrier.barrier();

      final StringWriter sw = (StringWriter) map.get("sw");
      final BufferedWriter bw = (BufferedWriter) map.get("bw");
      final ByteArrayOutputStream baos = (ByteArrayOutputStream) map.get("baos");
      final DataOutputStream dos = (DataOutputStream) map.get("dos");

      if (index == 0) {
        synchronized (bw) {
          bw.write("Hello");
          bw.write(" there");
          bw.flush();
        }

        synchronized (dos) {
          dos.writeUTF("Nothing beats a distributed DataOutputStream");
          dos.writeInt(42);
          dos.writeBoolean(false);
        }
      }

      barrier.barrier();

      if (index == 1) {
        Assert.assertEquals("Hello there", sw.toString());

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(dis.readUTF(), "Nothing beats a distributed DataOutputStream");
        Assert.assertEquals(42, dis.readInt());
        Assert.assertFalse(dis.readBoolean());
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = RandomIOClassesTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      spec.addRoot("map", "map");
      spec.addRoot("barrier", "barrier");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      new CyclicBarrierSpec().visit(visitor, config);
    }

  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializationResolveTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier;
    private SerializableObject  root;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {
      int index = barrier.barrier();
      if (index == 0) {
        root = makeGraph(500);
      }

      barrier.barrier();

      if (index != 0) {
        Object val = UninstrumentedReader.readField(root);
        if (val != null) { throw new AssertionError("failed to observe unresolved field"); }

        SerializableObject so = testSerialization(root);

        if (so == root) { throw new AssertionError("same object returned"); }

        if (ManagerUtil.isManaged(so)) { throw new AssertionError("deserialized object is shared"); }

        verifyGraph(so);
      }
    }

    private void verifyGraph(final SerializableObject top) {
      SerializableObject so = top;
      while (so != top) {
        if (ManagerUtil.isManaged(so)) { throw new AssertionError("deserialized object is shared"); }

        if (so.next == null) { throw new AssertionError("field was null in deserialized instance"); }

        so = so.next;
      }
    }

    private SerializableObject makeGraph(int depth) {
      SerializableObject top = new SerializableObject();
      SerializableObject current = top;
      for (int i = 0; i < depth; i++) {
        SerializableObject next = new SerializableObject();
        current.next = next;
        current = next;
      }
      current.next = top;
      return top;
    }

    private SerializableObject testSerialization(SerializableObject so) throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(so);
      oos.close();

      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
      return (SerializableObject) ois.readObject();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);

      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(testClass + "$*");
      config.addIncludePattern("com.tctest.SerializableObject");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
    }
  }
}

class SerializableObject implements Serializable {
  SerializableObject next;
}

class UninstrumentedReader {
  static Object readField(SerializableObject object) {
    // this field read is not instrumented and lets the test observe unresolved fields
    return object.next;
  }
}

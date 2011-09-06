/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test many fields in a physical object (CDV-1289)
 */
public class ManyFieldsTest extends TransparentTestBase {

  // XXX: we should work up to the maximum that a class file supports (64k I think!)
  // You'll get method length restrictions though on the methods that iterate all fields. This is in both the clietn and
  // the server
  private static final int NUM_FIELDS = 1927;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(2);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final Map           root    = new ConcurrentHashMap();
    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      IsolationClassLoader loader = (IsolationClassLoader) getClass().getClassLoader();
      loader.addAdapter("com.tctest.ManyFieldsTest$ManyFields", Adapter.class);

      int index = barrier.await();
      if (index == 0) {
        ManyFields many = new ManyFields();
        root.put("key", many);
        many.setValues();
      }

      barrier.await();

      ManyFields many = (ManyFields) root.get("key");

      many.check();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");

      config.addIncludePattern(ManyFields.class.getName());
      config.addWriteAutolock("* " + ManyFields.class.getName() + ".*(..)");
    }
  }

  private static class ManyFields {

    public synchronized void check() throws Exception {
      Field[] fields = getFields();

      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        field.setAccessible(true);
        AtomicInteger ai = (AtomicInteger) field.get(this);
        assertEquals(i, ai.get());
      }
    }

    public synchronized void setValues() throws Exception {
      Field[] fields = getFields();

      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        field.setAccessible(true);
        field.set(this, new AtomicInteger(i));
      }
    }

    private Field[] getFields() {
      List<Field> rv = new ArrayList<Field>();
      Field[] fields = getClass().getDeclaredFields();
      for (Field field : fields) {
        if (field.getName().startsWith("synthetic")) {
          rv.add(field);
        }
      }

      if (rv.size() != NUM_FIELDS) { throw new AssertionError("wrong numbre of fields found: " + rv.size()); }

      return rv.toArray(new Field[rv.size()]);

    }
  }

  private static class Adapter extends ClassAdapter {

    public Adapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visitEnd() {
      for (int i = 0; i < NUM_FIELDS; i++) {
        super.visitField(Opcodes.ACC_PRIVATE, "synthetic" + i, "Ljava/lang/Object;", null, null);
      }
      super.visitEnd();
    }
  }

}

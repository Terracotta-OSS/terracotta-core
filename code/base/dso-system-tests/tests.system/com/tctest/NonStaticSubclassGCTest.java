/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Opcodes;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;
import com.terracottatech.config.PersistenceMode;

import java.util.Arrays;
import java.util.List;

/**
 * This test simulates the bug described in https://jira.terracotta.org/jira/browse/CDV-765 The basic problem is with
 * class evolution involving non-static inner classes. Assume in one client there are shared types like this: <br>
 * <br>
 * <code>
 * class EnclosingType {
 * 
 * static abstract class AbstractBase {
 *   //
 * }
 * 
 * class NonStatic extends AbstractBase {
 *   //
 * }
 * </code> <br>
 * Then in another client a field is added in the AbstractBase class and shared. This test simulates this scenario <br>
 * <br>
 * Note: I'm pretty sure the same problem exists if simply a field is added to NonStatic, but I wanted to model the
 * exact the scenario experienced in the field
 */
public class NonStaticSubclassGCTest extends GCTestBase {

  private static final String GC_INTERVAL = NonStaticSubclassGCTest.class.getName() + ".GC_INTERVAL";

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(GC_INTERVAL, new Integer(gcConfigHelper.getGarbageCollectionInterval()));
    super.doSetUp(t);
  }

  protected int getNodeCount() {
    return 2;
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final int           gcInterval;
    private final CyclicBarrier barrier;
    private final Object[]      root = new Object[1]; // using an array since they are lazy loaded

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
      gcInterval = ((Integer) cfg.getAttributeObject(GC_INTERVAL)).intValue() * 1000;
    }

    @Override
    protected void runTest() throws Throwable {
      IsolationClassLoader loader = (IsolationClassLoader) getClass().getClassLoader();

      final int index = barrier.barrier();

      if (index == 1) {
        loader.addAdapter("com.tctest.NonStaticSubclassGCTest$App$AbstractBase", AddFieldAdapter.class);
      }

      barrier.barrier();

      verifyFields(index == 1);

      barrier.barrier();

      if (index == 0) {
        synchronized (root) {
          root[0] = new NonStaticInner();
        }
      }

      barrier.barrier();

      if (index != 0) {
        synchronized (root) {
          root[0] = new NonStaticInner();
        }
      }

      barrier.barrier();

      // sleep for a few DGC cycles to make sure DGC runs on these bad state objects
      ThreadUtil.reallySleep(gcInterval * 5);
    }

    private void verifyFields(boolean fieldsAdded) {
      List fields = Arrays.asList(AbstractBase.class.getDeclaredFields());
      boolean error = fieldsAdded ? fields.size() == 1 : fields.size() == 0;
      if (error) { throw new AssertionError("wrong number of fields (added: " + fieldsAdded + ") " + fields); }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
      config.addWriteAutolock("* " + testClassName + ".*(..)");
      config.addIncludePattern(AbstractBase.class.getName());
      config.addIncludePattern(NonStaticInner.class.getName());
    }

    abstract class AbstractBase {
      // a field will be added via instrumentation in the 2nd node
    }

    class NonStaticInner extends AbstractBase {
      //
    }

    private static class AddFieldAdapter extends ClassAdapter implements Opcodes {

      public AddFieldAdapter(ClassVisitor cv) {
        super(cv);
      }

      @Override
      public void visitEnd() {
        super.visitField(ACC_PUBLIC, "newField", "Ljava/lang/Object;", null, null);
        super.visitEnd();
      }
    }

  }

}

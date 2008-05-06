/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Test to make sure custom adapted class and its inherited classes can be shared under DSO
 *
 * @author hhuynh
 */
public class CustomClassAdapterTestApp extends AbstractErrorCatchingTransparentApp {
  private Map           root = new HashMap();
  private CyclicBarrier barrier;

  public CustomClassAdapterTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    Foo foo = new Foo();
    Assert.assertEquals(-1, foo.getVal());
    Assert.assertEquals(0, foo.getRealVal());
    foo.setVal(100);
    Assert.assertEquals(-1, foo.getVal());
    Assert.assertEquals(100, foo.getRealVal());

    FooKid fooKid = new FooKid();
    fooKid.setVal(100);
    Assert.assertEquals(-1, fooKid.getVal());
    Assert.assertEquals(-2, fooKid.getDoubleVal());

    if (barrier.barrier() == 0) {
      synchronized (root) {
        foo.setVal(200);
        fooKid.setVal(200);
        root.put("foo", foo);
        root.put("fooKid", fooKid);
      }
    }

    barrier.barrier();
    Foo sharedFoo = (Foo) root.get("foo");
    Assert.assertEquals(-1, foo.getVal());
    Assert.assertEquals(200, sharedFoo.getRealVal());

    FooKid sharedFooKid = (FooKid) root.get("fooKid");
    Assert.assertEquals(-1, fooKid.getVal());
    Assert.assertEquals(-2, sharedFooKid.getDoubleVal());

    // also make sure a class that happens to have a custom adapter isn't also
    // portable (ie. it must be included to be portable)
    AdaptedButNotIncluded a = new AdaptedButNotIncluded();
    Assert.assertEquals(-1, a.getVal()); // tests that adaption did happen
    try {
      synchronized (root) {
        root.put("test", new AdaptedButNotIncluded());
      }
      throw new AssertionError("Type is portable");
    } catch (TCNonPortableObjectError e) {
      // expected
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    CyclicBarrierSpec cbspec = new CyclicBarrierSpec();
    cbspec.visit(visitor, config);

    String testClass = CustomClassAdapterTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    config.addIncludePattern(Foo.class.getName());
    config.addIncludePattern(FooKid.class.getName());
    config.addCustomAdapter(Foo.class.getName(), new GetValAdapter());

    config.addCustomAdapter(AdaptedButNotIncluded.class.getName(), new GetValAdapter());
  }

}

class AdaptedButNotIncluded {
  private int val;

  public int getVal() {
    return val;
  }
}

class Foo {
  private int val;

  public int getVal() {
    return val;
  }

  public void setVal(int val) {
    this.val = val;
  }

  public int getRealVal() {
    return val;
  }
}

class FooKid extends Foo {
  public int getDoubleVal() {
    return getVal() * 2;
  }
}

class GetValAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public GetValAdapter() {
    super(null);
  }

  public GetValAdapter(ClassVisitor cv) {
    super(cv);
  }

  public GetValAdapter(ClassVisitor visitor, ClassLoader loader) {
    super(visitor);
  }

  public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                   final String[] exceptions) {
    if ("getVal".equals(name)) {
      MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "getVal", "()I", null, null);
      mv.visitCode();
      mv.visitInsn(ICONST_M1);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
      return null;
    }
    return cv.visitMethod(access, name, desc, signature, exceptions);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new GetValAdapter(visitor, loader);
  }
}

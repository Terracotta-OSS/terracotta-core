/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class IgnoreRewriteTestApp extends AbstractErrorCatchingTransparentApp {
  private final Map    myRoot = new HashMap(5, 1.0f);
  private final Object DUMMY  = new Object();

  public IgnoreRewriteTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  @Override
  protected void runTest() throws Throwable {
    ClassWithoutRewriteManageable classWithoutRewriteManageable = new ClassWithoutRewriteManageable();
    synchronized (myRoot) {
      myRoot.put(classWithoutRewriteManageable, DUMMY);
    }
    if (classWithoutRewriteManageable.rewriteCounter < 1) { throw new AssertionError(
                                                                                                 "classWithoutRewriteManageableCounter, should be non-zero is instead: "
                                                                                                     + classWithoutRewriteManageable.rewriteCounter); }
    
    ClassWithoutRewritePartialManageable classWithoutRewritePartialManageable = new ClassWithoutRewritePartialManageable();
    synchronized (myRoot) {
      myRoot.put(classWithoutRewritePartialManageable, DUMMY);
    }
    if (classWithoutRewritePartialManageable.rewriteCounter == 1) { throw new AssertionError(
                                                                                                 "classWithoutRewritePartialManageableCounter, should be 1 is instead: "
                                                                                                     + classWithoutRewritePartialManageable.rewriteCounter); }

    ClassWithRewriteManageable classWithRewriteManageable = new ClassWithRewriteManageable();
    synchronized (myRoot) {
      myRoot.put(classWithRewriteManageable, DUMMY);
    }
    if (classWithRewriteManageable.rewriteCounter > 0) { throw new AssertionError(
                                                                                              "classWithRewriteManageableCounter, should be zero is instead: "
                                                                                                  + classWithRewriteManageable.rewriteCounter); }

    ClassWithoutRewriteTransparentAccess classWithoutRewriteTransparentAccess = new ClassWithoutRewriteTransparentAccess();
    synchronized (myRoot) {
      myRoot.put(classWithoutRewriteTransparentAccess, DUMMY);
    }
    if (classWithoutRewriteTransparentAccess.rewriteCounter < 1) { throw new AssertionError(
                                                                                                        "classWithoutRewriteTransparentAccessCounter, should be non-zero is instead: "
                                                                                                            + classWithoutRewriteTransparentAccess.rewriteCounter); }

    ClassWithRewriteTransparentAccess classWithRewriteTransparentAccess = new ClassWithRewriteTransparentAccess();
    synchronized (myRoot) {
      myRoot.put(classWithRewriteTransparentAccess, DUMMY);
    }
    if (classWithRewriteTransparentAccess.rewriteCounter > 0) { throw new AssertionError(
                                                                                                     "classWithRewriteTransparentAccessCounter, should be zero is instead: "
                                                                                                         + classWithoutRewriteTransparentAccess.rewriteCounter); }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = IgnoreRewriteTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.setHonorTransient(true);
    spec.addRoot("myRoot", "myRoot");

    testClass = ClassWithoutRewriteManageable.class.getName();
    spec = config.getOrCreateSpec(testClass);
    spec.setIgnoreRewrite(true);
    spec.setHonorTransient(true);
    methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    
    testClass = ClassWithoutRewritePartialManageable.class.getName();
    spec = config.getOrCreateSpec(testClass);
    spec.setIgnoreRewrite(true);
    spec.setHonorTransient(true);
    methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    testClass = ClassWithRewriteManageable.class.getName();

    spec = config.getOrCreateSpec(testClass);
    spec.setHonorTransient(true);
    methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    
    

    testClass = ClassWithoutRewriteTransparentAccess.class.getName();
    spec = config.getOrCreateSpec(testClass);
    spec.setIgnoreRewrite(true);
    spec.setHonorTransient(true);
    methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    testClass = ClassWithRewriteTransparentAccess.class.getName();

    spec = config.getOrCreateSpec(testClass);
    spec.setHonorTransient(true);
    methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

  }

  private static class ClassWithoutRewriteManageable implements Manageable {

    private TCObject      $__TC_MANAGED;
    private transient int rewriteCounter = 0;

    public boolean __tc_isManaged() {
      rewriteCounter++;
      return $__TC_MANAGED != null;
    }

    public void __tc_managed(TCObject t) {
      rewriteCounter++;
      $__TC_MANAGED = t;
    }

    public TCObject __tc_managed() {
      rewriteCounter++;
      return $__TC_MANAGED;
    }

  }
  
  private static class ClassWithoutRewritePartialManageable {

    private transient int rewriteCounter = 0;

    @SuppressWarnings("unused")
    public boolean __tc_isManaged() {
      rewriteCounter++;
      return true;
    }

  }


  private static class ClassWithRewriteManageable implements Manageable {

    private TCObject      $__TC_MANAGED;
    private transient int rewriteCounter = 0;

    public boolean __tc_isManaged() {
      rewriteCounter++;
      return $__TC_MANAGED != null;
    }

    public void __tc_managed(TCObject t) {
      rewriteCounter++;
      $__TC_MANAGED = t;
    }

    public TCObject __tc_managed() {
      rewriteCounter++;
      return $__TC_MANAGED;
    }

  }

  private static class ClassWithoutRewriteTransparentAccess implements TransparentAccess {

    private transient int rewriteCounter = 0;

    public void __tc_getallfields(Map map) {
      rewriteCounter++;
    }

    public Object __tc_getmanagedfield(String name) {
      rewriteCounter++;
      return null;
    }

    public void __tc_setfield(String name, Object value) {
      rewriteCounter++;
    }

    public void __tc_setmanagedfield(String name, Object value) {
      rewriteCounter++;
    }

  }

  private static class ClassWithRewriteTransparentAccess implements TransparentAccess {

    private transient int rewriteCounter = 0;

    public void __tc_getallfields(Map map) {
      rewriteCounter++;
    }

    public Object __tc_getmanagedfield(String name) {
      rewriteCounter++;
      return null;
    }

    public void __tc_setfield(String name, Object value) {
      rewriteCounter++;
    }

    public void __tc_setmanagedfield(String name, Object value) {
      rewriteCounter++;
    }
  }

}

/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Eugene Kuleshov
 */
public class AnnotationTestApp extends AbstractTransparentApp {

  private ClassWithAnnotations value = new ClassWithAnnotations();

  public AnnotationTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    testLockOnAnnotation();
  }

  // CDV-271: Annotation support for Locks, includes by matching on what ever annotations they want.
  private void testLockOnAnnotation() {
    value.setDistributed("foo");
    junit.framework.Assert.assertEquals("foo", value.getDistributed());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(AnnotationTestApp.class.getName());
    spec.addRoot("value", "value");

    String testClass = "com.tctest.AnnotationTestApp$ClassWithAnnotations";
    config.addIncludePattern(testClass);
    
    config.addWriteAutolock("@com.tctest.AnnotationTestApp$WriteAutolock * " + testClass + ".*(..)");
    config.addReadAutolock("@com.tctest.AnnotationTestApp$ReadAutolock * " + testClass + ".*(..)");
  }
  
  
  public static class ClassWithAnnotations {
    private String distributed;
    
    @ReadAutolock
    public String getDistributed() {
      synchronized (this) {
        return distributed;
      }
    }
    
    @WriteAutolock
    public void setDistributed(String distributed) {
      synchronized (this) {
        this.distributed = distributed;
      }
    }
    
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface WriteAutolock {
    //
  }


  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface ReadAutolock {
    //
  }
  
}

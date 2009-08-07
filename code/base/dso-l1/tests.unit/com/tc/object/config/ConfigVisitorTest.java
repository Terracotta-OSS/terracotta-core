/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import junit.framework.TestCase;

public class ConfigVisitorTest extends TestCase {
  public void testDSOApplicationConfigVisitor() throws Throwable {

    ConfigVisitor visitor = new ConfigVisitor();
    TestDSOApplicationConfig cfg = new TestDSOApplicationConfig();
    visitor.visitDSOApplicationConfig(cfg, Target.class);
    Object[] args = (Object[]) Target.visitCalls.poll(0);
    assertNotNull(args);
    assertSame(visitor, args[0]);
    assertSame(cfg, args[1]);

    visitor.visitDSOApplicationConfig(cfg, Target.class);
    assertNull(Target.visitCalls.poll(0));
  }

  private static final class TestDSOApplicationConfig implements DSOApplicationConfig {
    public void addRoot(String rootName, String rootFieldName) {
      return;
    }

    public void writeTo(DSOApplicationConfigBuilder builder) {
      return;
    }

    public void addIncludePattern(String classPattern) {
      return;
    }

    public void addWriteAutolock(String methodPattern) {
      return;
    }

    public void addReadAutolock(String methodPattern) {
      return;
    }

    public void addIncludePattern(String classname, boolean b) {
      return;
    }
  }

  public static final class Target {
    public static NoExceptionLinkedQueue visitCalls = new NoExceptionLinkedQueue();

    public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
      visitCalls.put(new Object[] { visitor, config });
    }
  }

}

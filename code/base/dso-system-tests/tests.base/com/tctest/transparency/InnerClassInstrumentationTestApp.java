/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.transparency;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class InnerClassInstrumentationTestApp extends AbstractTransparentApp {

  private static final String INSTRUMENTED_KEY     = "instrumented";
  private static final String NOT_INSTRUMENTED_KEY = "not instrumented";

  private final Map           root                 = new HashMap();

  public InnerClassInstrumentationTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = InnerClassInstrumentationTestApp.class.getName();
    config.addIncludePattern(testClassName);
    config.addIncludePattern(MyInstrumentedInnerClass.class.getName());

    config.addIncludePattern(InnerInnerExtendsInstrumentedClass.class.getName());
    config.addIncludePattern(InnerInnerExtendsInstrumentedClass.InnerInnerClass.class.getName());
    config.addIncludePattern(InnerInnerExtendsInstrumentedClass.InnerInnerClass.InnerInnerInnerClass.class.getName());

    config.addIncludePattern(MyNonInstrumentedInnerSubclass.class.getName());

    config.addIncludePattern(InnerInnerExtendsNonInstrumentedClass.class.getName());
    config.addIncludePattern(InnerInnerExtendsNonInstrumentedClass.InnerInnerClass.class.getName());
    config
        .addIncludePattern(InnerInnerExtendsNonInstrumentedClass.InnerInnerClass.InnerInnerInnerClass.class.getName());

    config.addIncludePattern(InnerInnerStaticExtendsInstrumentedClass.class.getName());
    config.addIncludePattern(InnerInnerStaticExtendsInstrumentedClass.InnerInnerClass.class.getName());

    config.addIncludePattern(InnerInnerStaticExtendsNonInstrumentedClass.class.getName());
    config.addIncludePattern(InnerInnerStaticExtendsNonInstrumentedClass.InnerInnerClass.class.getName());

    config.addIncludePattern(AnonymousInstrumentedInnerInnerClass.class.getName());
    config.addIncludePattern(new AnonymousInstrumentedInnerInnerClass().getInnerInnerClassName());
    config.addIncludePattern(AnonymousNonInstrumentedInnerInnerClass.class.getName());
    config.addIncludePattern(new AnonymousNonInstrumentedInnerInnerClass().getInnerInnerClassName());
    config.addRoot(new Root(testClassName, "root", "root"), true);
    config.addAutolock("* " + testClassName + ".*(..)", ConfigLockLevel.WRITE);
  }

  public void run() {
    synchronized (root) {
      root.put(INSTRUMENTED_KEY, new MyInstrumentedInnerClass());
      try {
        root.put(NOT_INSTRUMENTED_KEY, new MyNonInstrumentedInnerClass());
        throw new AssertionError("This should have thrown an exception!");
      } catch (TCNonPortableObjectError t) {
        // expected.
      }
      root.put(INSTRUMENTED_KEY, new InnerInnerExtendsInstrumentedClass());
      try {
        root.put(NOT_INSTRUMENTED_KEY, new MyNonInstrumentedInnerSubclass());
        throw new AssertionError("This should have thrown an exception!");
      } catch (TCNonPortableObjectError t) {
        // expected.
      }

      try {
        root.put(NOT_INSTRUMENTED_KEY, new InnerInnerExtendsNonInstrumentedClass());
        throw new AssertionError("This should have thrown an exception!");
      } catch (TCNonPortableObjectError t) {
        // expected.
      }

      root.put(INSTRUMENTED_KEY, new InnerInnerStaticExtendsInstrumentedClass());

      try {
        root.put(NOT_INSTRUMENTED_KEY, new InnerInnerStaticExtendsNonInstrumentedClass());
        throw new AssertionError("This should have thrown an exception!");
      } catch (TCNonPortableObjectError t) {
        // expected.
      }

      root.put(INSTRUMENTED_KEY, new AnonymousInstrumentedInnerInnerClass());

      try {
        root.put(NOT_INSTRUMENTED_KEY, new AnonymousNonInstrumentedInnerInnerClass());
        throw new AssertionError("This should have thrown an exception!");
      } catch (TCNonPortableObjectError t) {
        // expected.
      }
    }
  }

  private static class MyNonInstrumentedInnerClass {
    //
  }

  private static class MyInstrumentedInnerClass {
    //
  }

  @SuppressWarnings("unused")
  private static final class InnerInnerExtendsInstrumentedClass {
    private final InnerInnerClass inner = new InnerInnerClass();

    private class InnerInnerClass extends MyInstrumentedInnerClass {
      private final InnerInnerInnerClass innerInner = new InnerInnerInnerClass();

      private class InnerInnerInnerClass extends MyInstrumentedInnerClass {
        //
      }
    }
  }

  @SuppressWarnings("unused")
  private static final class InnerInnerStaticExtendsInstrumentedClass {
    private final InnerInnerClass inner = new InnerInnerClass();

    private static class InnerInnerClass extends MyInstrumentedInnerClass {
      //
    }
  }

  private static final class MyNonInstrumentedInnerSubclass extends MyNonInstrumentedInnerClass {
    //
  }

  @SuppressWarnings("unused")
  private static final class InnerInnerExtendsNonInstrumentedClass {
    private final InnerInnerClass inner = new InnerInnerClass();

    private class InnerInnerClass extends MyInstrumentedInnerClass {
      private final InnerInnerInnerClass innerInner = new InnerInnerInnerClass();

      private class InnerInnerInnerClass extends MyNonInstrumentedInnerClass {
        //
      }
    }
  }

  @SuppressWarnings("unused")
  private static final class InnerInnerStaticExtendsNonInstrumentedClass {
    private final InnerInnerClass inner = new InnerInnerClass();

    private static class InnerInnerClass extends MyNonInstrumentedInnerClass {
      //
    }
  }

  private static final class AnonymousInstrumentedInnerInnerClass extends MyInstrumentedInnerClass {
    public String getInnerInnerClassName() {
      return innerInner.getClass().getName();
    }

    public final MyInstrumentedInnerClass innerInner = new MyInstrumentedInnerClass() {
                                                       //
                                                     };
  }

  private static final class AnonymousNonInstrumentedInnerInnerClass extends MyInstrumentedInnerClass {
    public String getInnerInnerClassName() {
      return innerInner.getClass().getName();
    }

    public final MyNonInstrumentedInnerClass innerInner = new MyNonInstrumentedInnerClass() {
                                                          //
                                                        };
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.LiteralValues;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.OverridesHashCode;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OverridesHashCodeSystemTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      verifyLiterals();
      verifyBasic();
    }

    private void verifyBasic() {
      Class doesNotOverride = DoesNotOverride.class;
      assertTrue(Manageable.class.isAssignableFrom(doesNotOverride));
      assertFalse(OverridesHashCode.class.isAssignableFrom(doesNotOverride));

      Class doesOverride = DoesOverride.class;
      assertTrue(Manageable.class.isAssignableFrom(doesOverride));
      assertTrue(OverridesHashCode.class.isAssignableFrom(doesOverride));
    }

    private void verifyLiterals() throws ClassNotFoundException {
      Collection<String> types = LiteralValues.getTypes();
      for (Iterator<String> iter = types.iterator(); iter.hasNext();) {

        String type = iter.next();

        // skip TC and primitive types
        if (type.startsWith("com.tc.") || type.indexOf('.') < 0) {
          continue;
        }

        if (!Vm.isJDK15Compliant() && type.equals(LiteralValues.ENUM_CLASS_DOTS)) {
          continue;
        }

        Class c = Class.forName(type);
        if (LITERALS_WITHOUT_HASHCODE.contains(type)) {
          assertFalse(c.getName(), OverridesHashCode.class.isAssignableFrom(c));
        } else {
          // If this assertion is going off you need to confirm if this literal type should have the interface or not
          // (If not you can add to the exclude set)
          assertTrue(c.getName(), OverridesHashCode.class.isAssignableFrom(c));
        }

      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addIncludePattern(DoesOverride.class.getName());
      config.addIncludePattern(DoesNotOverride.class.getName());
    }
  }

  private static class DoesNotOverride {
    // does not override hashCode()
  }

  private static class DoesOverride {
    public int hashCode() {
      return 3;
    }
  }

  private static final Set LITERALS_WITHOUT_HASHCODE = new HashSet();
  static {
    LITERALS_WITHOUT_HASHCODE.add(LiteralValues.ENUM_CLASS_DOTS);
    LITERALS_WITHOUT_HASHCODE.add(Currency.class.getName());
    LITERALS_WITHOUT_HASHCODE.add(Class.class.getName());
  }

}

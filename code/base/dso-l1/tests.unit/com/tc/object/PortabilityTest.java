/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.util.ClassUtils;
import com.tc.util.runtime.Vm;

import java.util.HashMap;

import junit.framework.TestCase;

public class PortabilityTest extends TestCase {

  public void testOverridesHashCode() {
    Portability p = new PortabilityImpl(null);
    assertTrue(p.overridesHashCode(new A()));
    assertTrue(p.overridesHashCode(new B()));
    assertTrue(p.overridesHashCode(new C()));

    assertTrue(p.overridesHashCode(new NativeHashcode()));

    assertTrue(p.overridesHashCode(new HashMap()));
    assertTrue(p.overridesHashCode(new Integer(1)));
    assertTrue(p.overridesHashCode("timmy"));

    // java.lang.Object has a hashCode method, but Portability should not say that it overrides it!
    assertFalse(p.overridesHashCode(new Object()));

    assertFalse(p.overridesHashCode(new D()));

    if (Vm.isJDK15Compliant()) {
      verifyEnum(p);
    }

  }

  private void verifyEnum(Portability p) {
    Class threadState = java.lang.Thread.State.class;

    assertTrue(ClassUtils.isDsoEnum(threadState));

    assertFalse(p.overridesHashCode(threadState));
  }

  static class A {
    // hashcode override
    public int hashCode() {
      return 1;
    }
  }

  static class B extends A {
    // parent overrides hashcode
  }

  static class C extends B {
    // one more level for good measure
  }

  static class D extends Object {
    // no hashcode override
  }

  static class NativeHashcode {
    public native int hashCode();
  }

}

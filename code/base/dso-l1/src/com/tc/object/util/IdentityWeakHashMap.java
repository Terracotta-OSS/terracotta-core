/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.util;

import sun.misc.MessageUtils;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

import java.util.Map;
import java.util.WeakHashMap;

public class IdentityWeakHashMap extends WeakHashMap {

  private static class TestKey {
    final int val;
    boolean   hashCodeCalled = false;
    boolean   equalsCalled   = false;

    TestKey(int val) {
      this.val = val;
    }

    public int hashCode() {
      // this shouldn't be happening, IdentityWeakHashMap should be using System.identityHashcode()
      hashCodeCalled = true;
      return val;
    }

    public boolean equals(Object obj) {
      // this shouldn't be happening, IdentityWeakHashMap should be using ==
      equalsCalled = true;
      return (obj instanceof TestKey) && ((TestKey) obj).val == this.val;
    }
  }

  static {
    /**
     * This static block is to validate the WeakHashMap superclass is the DSO instrumented version. If not, this class
     * will function incorrectly.
     */

    IdentityWeakHashMap m = new IdentityWeakHashMap();
    TestKey k1 = new TestKey(10); // strongly reference the Integer object so that the garbage collector
    TestKey k2 = new TestKey(10); // will not kick in before the check for m.size().
    m.put(k1, "first");
    m.put(k2, "second");
    if (m.size() != 2 || k1.hashCodeCalled || k1.equalsCalled || k2.hashCodeCalled || k1.equalsCalled) {
      ExceptionWrapper wrapper = new ExceptionWrapperImpl();
      String errorMsg = wrapper
          .wrap("WeakHashMap does not seem to contain the instrumented method.\n"
                + "IdentityWeakHashMap can only be used with the Terracotta instrumented version of WeakHashMap.\n"
                + "One possible cause is that WeakHashMap is not contained in the boot jar.");
      MessageUtils.toStderr(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  public IdentityWeakHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public IdentityWeakHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public IdentityWeakHashMap() {
    super();
  }

  public IdentityWeakHashMap(Map m) {
    super(m);
  }

  protected int __tc_hash(Object x) {
    int h = System.identityHashCode(x);

    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    return h;
  }

  protected boolean __tc_equal(Object obj1, Object obj2) {
    return obj1 == obj2;
  }

  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
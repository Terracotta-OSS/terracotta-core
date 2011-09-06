/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bootjar;

import com.tc.object.bytecode.MergeTCToJavaClassAdapter;
import com.tc.test.TCTestCase;

import junit.framework.Assert;

public class InnerClassesAttributeTest extends TCTestCase {

  public void testTimApiAdapterIsBroken() {
    Class<MergeTCToJavaClassAdapter> adapterClass = MergeTCToJavaClassAdapter.class;
    try {
      /*
       * This call should fail with a NoSuchMethodException as the FixedMergeTCToJavaClassAdapter subclass only exists
       * to fix the behavior of the current tim-api version of MergeTCToJavaClassAdapter. If MergeTCToJavaClassAdapter
       * now has a 'visitInnerClass' method it might be the new fixed tim-api version, and we can ditch the fixing
       * subclass.
       */
      adapterClass.getDeclaredMethod("visitInnerClass", String.class, String.class, String.class, Integer.TYPE);
      Assert.fail("MergeTCToJavaClassAdapter defines a 'visitInnerClass' method");
    } catch (NoSuchMethodException e) {
      // expected
    }
  }

  public void testReflectionOnAddedInnerClass() throws ClassNotFoundException {
    java.util.concurrent.locks.ReentrantReadWriteLock.class.getCanonicalName();
    Class.forName("java.util.concurrent.locks.ReentrantReadWriteLock$DsoLock").getCanonicalName();
  }
}

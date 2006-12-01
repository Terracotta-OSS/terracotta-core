/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.field.TCFieldFactory;

/**
 * TODO Nov 17, 2004: I, steve, am too lazy to write a single sentence describing what this class is for.
 */
public class TCClassTest extends BaseDSOTestCase {
  public void tests() throws Exception {
    // ClientObjectManager manager = new ClientObjectManagerImpl(null, null, null, null);

    DSOClientConfigHelper config = configHelper();
    TCFieldFactory fieldFactory = new TCFieldFactory(config);
    ClientObjectManager objectManager = new TestClientObjectManager();
    TCClassFactory classFactory = new TCClassFactoryImpl(fieldFactory, config, new MockClassProvider());
    TCClass tcc1 = new TCClassImpl(fieldFactory, classFactory, objectManager, TCClassTest.class, null, "", null, false, false, null, null, false);
    assertFalse(tcc1.isIndexed());
    assertFalse(tcc1.isNonStaticInner());
    TCClass tcc2 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1.class, null, "", null, false, false, null, null, false);
    assertFalse(tcc2.isIndexed());
    assertTrue(tcc2.isNonStaticInner());
    TCClass tcc3 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass2.class, null, "", null, false, false, null, null, false);
    assertFalse(tcc3.isIndexed());
    assertFalse(tcc3.isNonStaticInner());
    TCClass tcc4 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1[].class, null, "", null, true, false, null, null, false);
    assertTrue(tcc4.isIndexed());
    assertFalse(tcc4.isNonStaticInner());

    TCClass tcc5 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1[].class, null, "timmy", null, true, false, null,
                                   null, false);
    assertEquals("timmy", tcc5.getDefiningLoaderDescription());
  }

  private class TestClass1 {
    //
  }

  private static class TestClass2 {
    //
  }
}
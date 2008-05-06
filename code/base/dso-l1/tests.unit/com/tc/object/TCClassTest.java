/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.ClassProvider;

public class TCClassTest extends BaseDSOTestCase {
  public void tests() throws Exception {
    // ClientObjectManager manager = new ClientObjectManagerImpl(null, null, null, null);

    DSOClientConfigHelper config = configHelper();
    TCFieldFactory fieldFactory = new TCFieldFactory(config);
    ClientObjectManager objectManager = new TestClientObjectManager();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);
    TCClassFactory classFactory = new TCClassFactoryImpl(fieldFactory, config, classProvider, encoding);
    TCClass tcc1 = new TCClassImpl(fieldFactory, classFactory, objectManager, TCClassTest.class, null, "", null, false,
                                   false, null, null, false);
    assertFalse(tcc1.isIndexed());
    assertFalse(tcc1.isNonStaticInner());
    TCClass tcc2 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1.class, null, "", null, false,
                                   false, null, null, false);
    assertFalse(tcc2.isIndexed());
    assertTrue(tcc2.isNonStaticInner());
    TCClass tcc3 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass2.class, null, "", null, false,
                                   false, null, null, false);
    assertFalse(tcc3.isIndexed());
    assertFalse(tcc3.isNonStaticInner());
    TCClass tcc4 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1[].class, null, "", null, true,
                                   false, null, null, false);
    assertTrue(tcc4.isIndexed());
    assertFalse(tcc4.isNonStaticInner());

    TCClass tcc5 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1[].class, null, "timmy", null,
                                   true, false, null, null, false);
    assertEquals("timmy", tcc5.getDefiningLoaderDescription());
  }

  private class TestClass1 {
    //
  }

  private static class TestClass2 {
    //
  }
}
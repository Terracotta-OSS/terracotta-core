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

    final DSOClientConfigHelper config = configHelper();
    final TCFieldFactory fieldFactory = new TCFieldFactory(config);
    final ClientObjectManager objectManager = new TestClientObjectManager();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final TCClassFactory classFactory = new TCClassFactoryImpl(fieldFactory, config, classProvider, encoding, null,
                                                               null, null);
    final TCClass tcc1 = new TCClassImpl(fieldFactory, classFactory, objectManager, TCClassTest.class, null, null,
                                         false, false, false, null, null, false, true, null, null);
    assertFalse(tcc1.isIndexed());
    assertFalse(tcc1.isNonStaticInner());
    final TCClass tcc2 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1.class, null, null,
                                         false, false, false, null, null, false, true, null, null);
    assertFalse(tcc2.isIndexed());
    assertTrue(tcc2.isNonStaticInner());
    final TCClass tcc3 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass2.class, null, null,
                                         false, false, false, null, null, false, true, null, null);
    assertFalse(tcc3.isIndexed());
    assertFalse(tcc3.isNonStaticInner());
    final TCClass tcc4 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClass1[].class, null, null,
                                         true, false, false, null, null, false, true, null, null);
    assertTrue(tcc4.isIndexed());
    assertFalse(tcc4.isNonStaticInner());
  }

  public void testPortableFields() throws Exception {
    final DSOClientConfigHelper config = configHelper();
    final TCFieldFactory fieldFactory = new TCFieldFactory(config);
    final ClientObjectManager objectManager = new TestClientObjectManager();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final TCClassFactory classFactory = new TCClassFactoryImpl(fieldFactory, config, classProvider, encoding, null,
                                                               null, null);

    final TCClass tcc1 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestSuperclass1.class, null, null,
                                         false, false, false, null, null, false, true, null, null);
    assertEquals(2, tcc1.getPortableFields().length);

    final TCClass tcc2 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestSuperclass2.class, null, null,
                                         false, false, false, null, null, false, true, null, null);
    assertEquals(2, tcc2.getPortableFields().length);
    final TCClass tcc3 = new TCClassImpl(fieldFactory, classFactory, objectManager, TestClassPF.class, null, null,
                                         false, false, false, null, null, false, true, null, null);
    assertEquals(1, tcc3.getPortableFields().length);

  }

  private class TestClass1 {
    //
  }

  private static class TestClass2 {
    //
  }

  @SuppressWarnings("unused")
  private static class TestSuperclass1 {

    private final long L1 = 0;
    private final long L2 = 0;

  }

  @SuppressWarnings("unused")
  private static class TestSuperclass2 extends TestSuperclass1 {

    private final long L3 = 0;
    private final long L4 = 0;

  }

  @SuppressWarnings("unused")
  private static class TestClassPF extends TestSuperclass2 {

    private final long L5 = 0;

  }
}

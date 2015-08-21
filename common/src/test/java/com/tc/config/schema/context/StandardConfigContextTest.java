/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package com.tc.config.schema.context;

import com.tc.config.schema.repository.MockBeanRepository;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link StandardConfigContext}.
 */
public class StandardConfigContextTest extends TCTestCase {

  private MockBeanRepository beanRepository;

  private ConfigContext context;

  @Override
  public void setUp() throws Exception {
    this.beanRepository = new MockBeanRepository();

    this.context = new StandardConfigContext(this.beanRepository);
  }

  public void testEnsureRepositoryProvides() throws Exception {
    this.beanRepository.setExceptionOnEnsureBeanIsOfClass(null);

    this.context.ensureRepositoryProvides(Number.class);
    assertEquals(1, this.beanRepository.getNumEnsureBeanIsOfClasses());
    assertEquals(Number.class, this.beanRepository.getLastClass());
    this.beanRepository.reset();

    RuntimeException exception = new RuntimeException("foo");
    this.beanRepository.setExceptionOnEnsureBeanIsOfClass(exception);

    try {
      this.context.ensureRepositoryProvides(Object.class);
      fail("Didn't get expected exception");
    } catch (RuntimeException re) {
      assertSame(exception, re);
      assertEquals(1, this.beanRepository.getNumEnsureBeanIsOfClasses());
      assertEquals(Object.class, this.beanRepository.getLastClass());
    }
  }

  @SuppressWarnings("unused")
  public void testConstruction() throws Exception {
    try {
      new StandardConfigContext(null);
      fail("Didn't get NPE on no bean repository");
    } catch (NullPointerException npe) {
      // ok
    }
  }


  public void testBean() throws Exception {
    Object object = new Object();
    this.beanRepository.setReturnedBean(object);

    assertSame(object, this.context.bean());
    assertEquals(1, this.beanRepository.getNumBeans());
  }

}

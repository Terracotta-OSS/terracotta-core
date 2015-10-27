/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

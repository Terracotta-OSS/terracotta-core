/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import com.tc.config.schema.MockApplication;
import com.tc.config.schema.validate.MockConfigurationValidator;
import com.tc.test.TCTestCase;
import com.terracottatech.config.Application;

/**
 * Unit test for {@link StandardApplicationsRepository}.
 */
public class StandardApplicationsRepositoryTest extends TCTestCase {

  private StandardApplicationsRepository repository;
  private MockConfigurationValidator     validator1;
  private MockConfigurationValidator     validator2;

  public void setUp() throws Exception {
    this.repository = new StandardApplicationsRepository();

    this.validator1 = new MockConfigurationValidator();
    this.validator2 = new MockConfigurationValidator();
  }

  public void testAll() throws Exception {
    try {
      this.repository.repositoryFor(null);
      fail("Didn't get NPE on no application name");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.repository.repositoryFor("");
      fail("Didn't get IAE on empty application name");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      this.repository.repositoryFor("    ");
      fail("Didn't get IAE on blank application name");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    assertEqualsUnordered(new String[0], this.repository.applicationNames());

    MutableBeanRepository one = this.repository.repositoryFor("one");

    assertEqualsUnordered(new String[] { "one" }, this.repository.applicationNames());

    this.repository.addRepositoryValidator(this.validator1);

    MutableBeanRepository two = this.repository.repositoryFor("two");
    assertSame(two, this.repository.repositoryFor("two"));
    assertNotSame(one, two);

    assertSame(one, this.repository.repositoryFor("one"));
    assertSame(two, this.repository.repositoryFor("two"));
    assertSame(one, this.repository.repositoryFor("one"));
    assertSame(two, this.repository.repositoryFor("two"));

    assertEqualsUnordered(new String[] { "one", "two" }, this.repository.applicationNames());

    this.repository.addRepositoryValidator(this.validator2);

    MutableBeanRepository three = this.repository.repositoryFor("three");

    assertEqualsUnordered(new String[] { "one", "two", "three" }, this.repository.applicationNames());
    assertNotSame(one, three);
    assertNotSame(two, three);

    assertSame(three, this.repository.repositoryFor("three"));

    Application oneBean = new MockApplication();
    Application twoBean = new MockApplication();
    Application threeBean = new MockApplication();

    one.setBean(oneBean, "foobar");

    assertEquals(0, this.validator1.getNumValidates());
    assertEquals(0, this.validator2.getNumValidates());

    two.setBean(twoBean, "foobaz");

    assertEquals(1, this.validator1.getNumValidates());
    assertSame(twoBean, this.validator1.getLastBean());
    assertEquals(0, this.validator2.getNumValidates());

    this.validator1.reset();

    three.setBean(threeBean, "foobonk");

    assertEquals(1, this.validator1.getNumValidates());
    assertSame(threeBean, this.validator1.getLastBean());
    assertEquals(1, this.validator2.getNumValidates());
    assertSame(threeBean, this.validator2.getLastBean());

    this.validator1.reset();
  }

}

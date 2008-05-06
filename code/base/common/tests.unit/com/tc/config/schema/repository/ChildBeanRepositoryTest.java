/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.listen.MockConfigurationChangeListener;
import com.tc.test.TCTestCase;
import com.tc.util.TCAssertionError;

/**
 * Unit test for {@link ChildBeanRepository}.
 */
public class ChildBeanRepositoryTest extends TCTestCase {

  // A child class, just to test that we check the actual class of the bean we're returning.
  private static class MyMockXmlObject extends MockXmlObject {
    public MyMockXmlObject() {
      super();
    }
  }

  private MockBeanRepository              parent;
  private Class                           requiredClass;
  private MockChildBeanFetcher            childFetcher;

  private ChildBeanRepository             repository;

  private MockConfigurationChangeListener listener1;
  private MockConfigurationChangeListener listener2;

  public void setUp() throws Exception {
    this.parent = new MockBeanRepository();
    this.requiredClass = MyMockXmlObject.class;
    this.childFetcher = new MockChildBeanFetcher();

    this.repository = new ChildBeanRepository(this.parent, this.requiredClass, this.childFetcher);

    this.listener1 = new MockConfigurationChangeListener();
    this.listener2 = new MockConfigurationChangeListener();
  }

  public void testConstruction() throws Exception {
    try {
      new ChildBeanRepository(null, this.requiredClass, this.childFetcher);
      fail("Didn't get NPE on no parent");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new ChildBeanRepository(this.parent, null, this.childFetcher);
      fail("Didn't get NPE on no required class");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new ChildBeanRepository(this.parent, this.requiredClass, null);
      fail("Didn't get NPE on no child fetcher");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testRequiredBeanClass() throws Exception {
    this.repository.ensureBeanIsOfClass(MyMockXmlObject.class);
    this.repository.ensureBeanIsOfClass(MockXmlObject.class);
    this.repository.ensureBeanIsOfClass(Object.class);

    try {
      this.repository.ensureBeanIsOfClass(String.class);
      fail("Didn't get TCAE on wrong class");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testInitiallyAddsListener() throws Exception {
    assertEquals(1, this.parent.getNumAddListeners());
    assertSame(this.repository, this.parent.getLastListener());
  }

  public void testBean() throws Exception {
    MockXmlObject theParent = new MockXmlObject();
    MyMockXmlObject child = new MyMockXmlObject();

    this.parent.setReturnedBean(theParent);
    this.childFetcher.setReturnedChild(child);

    assertSame(child, this.repository.bean());
    assertEquals(1, this.parent.getNumBeans());
    assertEquals(1, this.childFetcher.getNumGetChilds());
    assertSame(theParent, this.childFetcher.getLastParent());

    this.parent.reset();
    this.childFetcher.reset();

    MockXmlObject childOfWrongClass = new MockXmlObject();
    this.childFetcher.setReturnedChild(childOfWrongClass);

    try {
      this.repository.bean();
      fail("Didn't get TCAE on child of wrong class");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testListeners() throws Exception {
    this.repository.addListener(this.listener1);
    this.repository.addListener(this.listener2);

    MockXmlObject parent1 = new MockXmlObject();
    MockXmlObject parent2 = new MockXmlObject();
    MyMockXmlObject child1 = new MyMockXmlObject();
    MyMockXmlObject child2 = new MyMockXmlObject();

    this.parent.setReturnedBean(parent1);
    this.childFetcher.setReturnedChildren(new XmlObject[] { child1, child2 });

    this.repository.configurationChanged(parent1, parent2);

    assertEquals(1, this.listener1.getNumConfigurationChangeds());
    assertSame(child1, this.listener1.getLastOldConfig());
    assertSame(child2, this.listener2.getLastNewConfig());

    assertEquals(2, this.childFetcher.getNumGetChilds());
    assertEqualsUnordered(new Object[] { parent1, parent2 }, this.childFetcher.getLastParents());

    this.listener1.reset();
    this.listener2.reset();
    this.childFetcher.reset();

    this.childFetcher.setReturnedChildren(new XmlObject[] { child1, child1 });

    this.repository.configurationChanged(parent1, parent2);
    assertEquals(2, this.childFetcher.getNumGetChilds());
    assertEqualsUnordered(new Object[] { parent1, parent2 }, this.childFetcher.getLastParents());

    assertEquals(0, this.listener1.getNumConfigurationChangeds());
    assertEquals(0, this.listener2.getNumConfigurationChangeds());

    this.childFetcher.setReturnedChildren(new XmlObject[] { child1, new MockXmlObject() });

    try {
      this.repository.configurationChanged(parent1, parent2);
      fail("Didn't get TCAE on wrong new child class");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.schema.MockSchemaType;
import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.listen.MockConfigurationChangeListener;
import com.tc.config.schema.validate.MockConfigurationValidator;
import com.tc.test.TCTestCase;
import com.tc.util.TCAssertionError;

/**
 * Unit test for {@link StandardBeanRepository}.
 */
public class StandardBeanRepositoryTest extends TCTestCase {

  private static class MyXmlObject extends MockXmlObject {
    // DO NOT REMOVE THIS TO FIX THE WARNING. We have to have public static final 'type' fields on both MockXmlObject
    // and MyXmlObject, because that's the way XMLBeans does it; various classes use reflection to find this, and so
    // you'll break tests if you change it.
    @SuppressWarnings("unused")
    public static final SchemaType schemaType = new MockSchemaType();

    private boolean                returnedValidate;

    public MyXmlObject() {
      super();
      this.returnedValidate = true;
    }

    public void setReturnedValidate(boolean validate) {
      this.returnedValidate = validate;
    }

    @Override
    public boolean validate(XmlOptions arg0) {
      return this.returnedValidate;
    }
  }

  private StandardBeanRepository          repository;

  private MockConfigurationChangeListener listener1;
  private MockConfigurationChangeListener listener2;

  private MockConfigurationValidator      validator1;
  private MockConfigurationValidator      validator2;

  @Override
  public void setUp() throws Exception {
    this.repository = new StandardBeanRepository(MyXmlObject.class);

    this.listener1 = new MockConfigurationChangeListener();
    this.listener2 = new MockConfigurationChangeListener();

    this.validator1 = new MockConfigurationValidator();
    this.validator2 = new MockConfigurationValidator();
  }

  public void testConstruction() throws Exception {
    try {
      new StandardBeanRepository(null);
      fail("Didn't get NPE on no required class");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testComponents() throws Exception {
    this.repository.ensureBeanIsOfClass(MyXmlObject.class);
    this.repository.ensureBeanIsOfClass(MockXmlObject.class);
    this.repository.ensureBeanIsOfClass(Object.class);

    try {
      this.repository.ensureBeanIsOfClass(String.class);
      fail("Didn't get TCAE on wrong bean class");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testAll() throws Exception {
    MyXmlObject bean1 = new MyXmlObject();
    MyXmlObject bean2 = new MyXmlObject();

    try {
      this.repository.setBean(bean1, null);
      fail("Didn't get NPE on no source");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.repository.setBean(bean1, "");
      fail("Didn't get IAE on empty source");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      this.repository.setBean(bean1, "   ");
      fail("Didn't get IAE on blanksource");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      this.repository.setBean(new MockXmlObject(), "foobar");
      fail("Didn't get TCAE on wrong class");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      this.repository.addListener(null);
      fail("Didn't get NPE on no listener");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.repository.addValidator(null);
      fail("Didn't get NPE on no validator");
    } catch (NullPointerException npe) {
      // ok
    }

    this.repository.setBean(bean1, "foobar");
    assertSame(bean1, this.repository.bean());

    this.repository = new StandardBeanRepository(MyXmlObject.class);

    this.repository.addListener(listener1);
    this.repository.addListener(listener2);
    this.repository.addValidator(validator1);
    this.repository.addValidator(validator2);

    checkNoListeners();

    this.repository.setBean(bean1, "foobar");
    checkListeners(null, bean1);

    this.repository.setBean(bean2, "baz");
    checkListeners(bean1, bean2);

    this.repository.setBean(null, "bonk");
    checkListeners(bean2, null);

    this.repository.setBean(bean2, "bonk");
    checkListeners(null, bean2);

    bean1.setReturnedValidate(false);
    try {
      this.repository.setBean(bean1, "foo");
      fail("Didn't get XmlException on failed schema validation");
    } catch (XmlException xmle) {
      // ok
    }
    assertSame(bean2, this.repository.bean());
    checkNoListeners();

    bean1.setReturnedValidate(true);
    validator1.setThrownException(new XmlException("fooBAR"));
    try {
      this.repository.setBean(bean1, "quux");
      fail("Didn't get XmlException on failed validator validation");
    } catch (XmlException xmle) {
      assertContains("fooBAR", xmle.getMessage());
    }
    assertSame(bean2, this.repository.bean());
    checkNoListeners();

    validator1.setThrownException(null);
    validator2.setThrownException(null);

    this.repository.setBean(bean1, "Whatever");
    assertSame(bean1, this.repository.bean());
    checkListeners(bean2, bean1);
  }

  private void checkNoListeners() {
    assertEquals(0, this.listener1.getNumConfigurationChangeds());
    assertEquals(0, this.listener2.getNumConfigurationChangeds());
  }

  private void checkListeners(XmlObject expectedOld, XmlObject expectedNew) {
    assertEquals(1, this.listener1.getNumConfigurationChangeds());
    assertSame(expectedOld, this.listener1.getLastOldConfig());
    assertSame(expectedNew, this.listener1.getLastNewConfig());

    assertEquals(1, this.listener2.getNumConfigurationChangeds());
    assertSame(expectedOld, this.listener2.getLastOldConfig());
    assertSame(expectedNew, this.listener2.getLastNewConfig());

    this.listener1.reset();
    this.listener2.reset();
  }

}

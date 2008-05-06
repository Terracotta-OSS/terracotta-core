/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.beanfactory;

import org.apache.xmlbeans.XmlError;

import com.tc.config.schema.MockXmlObject;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link BeanWithErrors}.
 */
public class BeanWithErrorsTest extends TCTestCase {

  private MockXmlObject xmlObject;
  private XmlError[]    errors;
  
  private BeanWithErrors beanWithErrors;

  public void setUp() throws Exception {
    this.xmlObject = new MockXmlObject();
    this.errors = new XmlError[] { XmlError.forMessage("foobar"), XmlError.forMessage("bazbar") };
    
    this.beanWithErrors = new BeanWithErrors(this.xmlObject, this.errors);
  }

  public void testConstruction() throws Exception {
    try {
      new BeanWithErrors(null, this.errors);
      fail("Didn't get NPE on no object");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new BeanWithErrors(this.xmlObject, null);
      fail("Didn't get NPE on no errors");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new BeanWithErrors(this.xmlObject, new XmlError[] { this.errors[0], null, this.errors[1] });
      fail("Didn't get NPE on null error");
    } catch (NullPointerException npe) {
      // ok
    }
  }
  
  public void testComponents() throws Exception {
    assertSame(this.xmlObject, this.beanWithErrors.bean());
    assertSame(this.errors, this.beanWithErrors.errors());
  }

}

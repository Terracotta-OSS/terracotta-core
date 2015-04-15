/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
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

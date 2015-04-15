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
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.context.MockConfigContext;
import com.tc.test.TCTestCase;

/**
 * A base for all {@link XPathBasedConfigItem} tests.
 */
public abstract class XPathBasedConfigItemTestBase extends TCTestCase {

  protected MockConfigContext context;
  protected String            xpath;

  protected MockXmlObject     bean;
  protected MockXmlObject     subBean;

  @Override
  protected void setUp() throws Exception {
    this.context = new MockConfigContext();
    this.xpath = "foobar/baz";

    this.bean = new MockXmlObject();
    this.context.setReturnedBean(this.bean);

    this.subBean = createSubBean();
    this.bean.setReturnedSelectPath(new XmlObject[] { this.subBean });
  }

  protected abstract MockXmlObject createSubBean() throws Exception;

}

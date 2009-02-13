/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.beanfactory;

import org.xml.sax.SAXParseException;

import com.tc.util.Assert;

/**
 * An XML bean, plus a list of errors.
 */
public class BeanWithErrors {

  private final Object bean;
  private final SAXParseException[] errors;

  public BeanWithErrors(Object bean, SAXParseException[] errors) {
    Assert.assertNotNull(bean);
    Assert.assertNoNullElements(errors);

    this.bean = bean;
    this.errors = errors;
  }

  public Object bean() {
    return this.bean;
  }

  public SAXParseException[] errors() {
    return this.errors;
  }

}

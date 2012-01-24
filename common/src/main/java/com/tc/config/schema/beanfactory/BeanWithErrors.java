/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.beanfactory;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;

/**
 * An XML bean, plus a list of errors.
 */
public class BeanWithErrors {
  
  private final XmlObject bean;
  private final XmlError[] errors;
  
  public BeanWithErrors(XmlObject bean, XmlError[] errors) {
    Assert.assertNotNull(bean);
    Assert.assertNoNullElements(errors);
    
    this.bean = bean;
    this.errors = errors;
  }
  
  public XmlObject bean() {
    return this.bean;
  }
  
  public XmlError[] errors() {
    return this.errors;
  }

}

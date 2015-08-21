/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.repository;

import com.tc.util.Assert;

/**
 * The standard implementation of {@link MutableBeanRepository}.
 */
public class StandardBeanRepository implements BeanRepository {

  private final Class<?>                       requiredClass;
  private Object                               bean;

  public StandardBeanRepository(Class<?> requiredClass) {
    Assert.assertNotNull(requiredClass);

    this.requiredClass = requiredClass;
    this.bean = null;
  }

  @Override
  public void ensureBeanIsOfClass(Class<?> theClass) {
    if (!theClass.isAssignableFrom(this.requiredClass)) {
      // formatting
      throw Assert.failure("You're making sure this repository requires at least " + theClass + ", but it requires "
                           + this.requiredClass + ", which isn't that class or a subclass thereof.");
    }
  }

  @Override
  public synchronized Object bean() {
    return this.bean;
  }
  
  @Override
  public synchronized void setBean(Object bean, String description) {
    this.bean = bean;    
  }

  @Override
  public String toString() {
    return "<Repository for bean of class " + this.requiredClass.getSimpleName() + "; have bean? "
           + (this.bean != null) + ">";
  }

}

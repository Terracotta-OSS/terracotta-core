/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.repository;

import com.tc.util.Assert;

/**
 * A {@link BeanRepository} that selects out a child of the parent bean.
 */
public class ChildBeanRepository implements BeanRepository {

  private final BeanRepository parent;
  private final Class<?> requiredBeanClass;
  private final ChildBeanFetcher childFetcher;

  public ChildBeanRepository(BeanRepository parent, Class<?> requiredBeanClass, ChildBeanFetcher childFetcher) {
    Assert.assertNotNull(parent);
    Assert.assertNotNull(requiredBeanClass);
    Assert.assertNotNull(childFetcher);

    this.parent = parent;
    this.requiredBeanClass = requiredBeanClass;
    this.childFetcher = childFetcher;
  }

  @Override
  public void ensureBeanIsOfClass(Class<?> theClass) {
    Assert.eval(theClass.isAssignableFrom(requiredBeanClass));
  }

  @Override
  public Object bean() {
    Object parentBean = this.parent.bean();
    if (parentBean == null) return null;
    Object out = this.childFetcher.getChild(parentBean);
    if (out != null) {
      Assert.eval("Child bean fetcher returned " + "a " + out.getClass() + ", not a " + this.requiredBeanClass,
                  this.requiredBeanClass.isInstance(out));
    }
    return out;
  }

  @Override
  public void setBean(Object bean, String description) {
    throw new UnsupportedOperationException();    
  }

}

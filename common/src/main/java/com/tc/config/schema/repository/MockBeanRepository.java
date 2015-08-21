/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package com.tc.config.schema.repository;

/**
 * A mock {@link BeanRepository}, for use in tests.
 */
public class MockBeanRepository implements BeanRepository {

  private int numEnsureBeanIsOfClasses;
  private Class<?> lastClass;
  private RuntimeException exceptionOnEnsureBeanIsOfClass;

  private int numBeans;
  private Object returnedBean;

  private int numSetBeans;
  private Object lastSetBean;
  private String lastSourceDescription;


  public MockBeanRepository() {
    this.returnedBean = null;
    this.exceptionOnEnsureBeanIsOfClass = null;

    reset();
  }

  public void reset() {
    this.numEnsureBeanIsOfClasses = 0;
    this.lastClass = null;

    this.numBeans = 0;

    this.numSetBeans = 0;
    this.lastSetBean = null;
    this.lastSourceDescription = null;
  }

  public void setExceptionOnEnsureBeanIsOfClass(RuntimeException exceptionOnEnsureBeanIsOfClass) {
    this.exceptionOnEnsureBeanIsOfClass = exceptionOnEnsureBeanIsOfClass;
  }

  @Override
  public void ensureBeanIsOfClass(Class<?> theClass) {
    ++this.numEnsureBeanIsOfClasses;
    this.lastClass = theClass;
    if (this.exceptionOnEnsureBeanIsOfClass != null) throw this.exceptionOnEnsureBeanIsOfClass;
  }

  @Override
  public Object bean() {
    ++this.numBeans;
    return this.returnedBean;
  }

  public Object getLastSetBean() {
    return lastSetBean;
  }

  public String getLastSourceDescription() {
    return lastSourceDescription;
  }

  public int getNumBeans() {
    return numBeans;
  }

  public int getNumSetBeans() {
    return numSetBeans;
  }

  public void setReturnedBean(Object returnedBean) {
    this.returnedBean = returnedBean;
  }

  public Class<?> getLastClass() {
    return lastClass;
  }

  public int getNumEnsureBeanIsOfClasses() {
    return numEnsureBeanIsOfClasses;
  }

  @Override
  public void setBean(Object bean, String description) {
    throw new UnsupportedOperationException();
  }

}

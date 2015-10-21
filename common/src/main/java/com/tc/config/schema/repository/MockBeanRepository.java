/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

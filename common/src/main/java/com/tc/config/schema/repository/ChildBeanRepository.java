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

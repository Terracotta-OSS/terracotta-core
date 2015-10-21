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
package com.tc.config.schema.context;

import com.tc.config.schema.repository.BeanRepository;
import com.tc.util.Assert;

/**
 * Binds together a {@link BeanRepository} and a {@link DefaultValueProvider}.
 */
public class StandardConfigContext implements ConfigContext {

  private final BeanRepository                    beanRepository;

  public StandardConfigContext(BeanRepository beanRepository) {
    Assert.assertNotNull(beanRepository);

    this.beanRepository = beanRepository;
  }

  @Override
  public void ensureRepositoryProvides(Class<?> theClass) {
    beanRepository.ensureBeanIsOfClass(theClass);
  }

  @Override
  public Object bean() {
    return this.beanRepository.bean();
  }

  @Override
  public String toString() {
    return "<ConfigContext around repository: " + this.beanRepository + ">";
  }

}

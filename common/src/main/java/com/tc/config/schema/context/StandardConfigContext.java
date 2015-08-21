/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.context;

import com.tc.config.schema.repository.BeanRepository;

/**
 * Binds together a {@link BeanRepository} and a {@link DefaultValueProvider}, and provides convenience methods for
 * creating various items.
 */
public interface ConfigContext {

  void ensureRepositoryProvides(Class<?> theClass);

  Object bean();

}

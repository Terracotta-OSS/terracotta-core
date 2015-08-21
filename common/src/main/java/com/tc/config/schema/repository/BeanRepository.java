/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.repository;

/**
 * An object that holds onto an {@link XmlObject}, and lets you know when it changes.
 */
public interface BeanRepository {

  void ensureBeanIsOfClass(Class<?> theClass);

  Object bean();

  void setBean(Object bean, String description);

}

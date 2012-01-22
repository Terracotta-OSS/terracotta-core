/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.listen.ConfigurationChangeListener;

/**
 * An object that holds onto an {@link XmlObject}, and lets you know when it changes.
 */
public interface BeanRepository {

  void ensureBeanIsOfClass(Class theClass);

  SchemaType rootBeanSchemaType();

  XmlObject bean();

  void addListener(ConfigurationChangeListener listener);

}

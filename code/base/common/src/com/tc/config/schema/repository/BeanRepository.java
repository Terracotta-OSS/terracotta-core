/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

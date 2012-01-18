/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import com.tc.config.schema.builder.RootConfigBuilder;

/**
 * Allows you to build valid config for a root. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class RootConfigBuilderImpl extends BaseConfigBuilder implements RootConfigBuilder {

  public RootConfigBuilderImpl(Class clazz, String field) {
    this();
    setFieldName(clazz.getName() + "." + field);
  }

  public RootConfigBuilderImpl(Class clazz, String fieldName, String rootName) {
    this();
    setFieldName(clazz.getName() + "." + fieldName);
    setRootName(rootName);
  }

  public RootConfigBuilderImpl() {
    super(4, ALL_PROPERTIES);
  }

  public void setFieldName(String name) {
    setProperty("field-name", name);
  }

  public void setRootName(String name) {
    setProperty("root-name", name);
  }

  private static final String[] ALL_PROPERTIES = new String[] { "field-name", "root-name" };

  public String toString() {
    return elements(ALL_PROPERTIES);
  }

}

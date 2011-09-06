/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.WebApplicationConfigBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to create a web-application config element.
 */
public class WebApplicationConfigBuilderImpl extends BaseConfigBuilder implements WebApplicationConfigBuilder {

  private static final String TAG_NAME = "web-application";

  private Map                 attributes;

  public WebApplicationConfigBuilderImpl() {
    super(5, new String[] { TAG_NAME });
    attributes = new HashMap();
  }

  public void setWebApplicationName(String name) {
    setProperty(TAG_NAME, name);
  }

  public void setWebApplicationAttributes(Map attributes) {
    this.attributes.putAll(attributes);
  }

  public String toString() {
    return openElement(TAG_NAME, attributes) + propertyAsString(TAG_NAME) + closeElement(TAG_NAME);
  }

}

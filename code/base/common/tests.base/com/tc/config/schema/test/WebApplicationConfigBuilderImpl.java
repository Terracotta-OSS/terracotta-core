/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.WebApplicationConfigBuilder;

/**
 * Used to create a web-application config element.
 */
public class WebApplicationConfigBuilderImpl extends BaseConfigBuilder implements WebApplicationConfigBuilder {

  public void setWebApplicationName(String name) {
    setProperty("web-application", name);
  }

  public WebApplicationConfigBuilderImpl() {
    super(5, ALL_PROPERTIES);
  }
  
  private static final String[] ALL_PROPERTIES = new String[] { "web-application" };
  
  public String toString() {
    return elements(ALL_PROPERTIES);
  }

}

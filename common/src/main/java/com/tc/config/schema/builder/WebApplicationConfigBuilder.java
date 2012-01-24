/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.builder;

import java.util.Map;

public interface WebApplicationConfigBuilder {
  public static final String ATTRIBUTE_NAME = "synchronous-write";

  void setWebApplicationName(String name);

  void setWebApplicationAttributes(Map attributes);
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.builder;

import java.util.Map;

public interface WebApplicationConfigBuilder {
  public static final String ATTRIBUTE_NAME = "synchronous-write";

  void setWebApplicationName(String name);

  void setWebApplicationAttributes(Map attributes);
}

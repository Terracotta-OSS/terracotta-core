/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.builder;


public interface SpringApplicationConfigBuilder {

  public String toString();

  public SpringApplicationContextConfigBuilder[] getApplicationContexts();

  public void setName(String name);

  public void setApplicationContexts(SpringApplicationContextConfigBuilder[] builders);

}
/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config.schema;

/**
 * Represents a class instrumentation pattern.
 */
public interface InstrumentedClass {

  boolean isInclude();

  String classExpression();

  boolean honorTransient();
  
  public boolean honorVolatile();

  public IncludeOnLoad onLoad();

}

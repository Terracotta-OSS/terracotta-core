/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

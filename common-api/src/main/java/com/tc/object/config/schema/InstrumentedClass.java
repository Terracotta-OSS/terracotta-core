/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

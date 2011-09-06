/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

public class TestInstrumentedClass implements InstrumentedClass {

  public boolean isInclude() {

    return false;
  }

  public String classExpression() {

    return null;
  }

  public boolean honorTransient() {

    return false;
  }
  
  public boolean honorVolatile() {
    
    return false;
  }

  public IncludeOnLoad onLoad() {

    return null;
  }

}

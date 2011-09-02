/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.builder;

public interface InstrumentedClassConfigBuilder {

  public void setIsInclude(boolean isInclude);

  public void setClassExpression(String value);

  public void setHonorTransient(String value);

  public void setHonorTransient(boolean value);

  public void setCallConstructorOnLoad(String value);

  public void setCallConstructorOnLoad(boolean value);
  
  public void setCallMethodOnLoad(String value);

}
/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

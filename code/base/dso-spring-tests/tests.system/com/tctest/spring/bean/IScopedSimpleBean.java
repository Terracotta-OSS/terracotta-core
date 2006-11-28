/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public interface IScopedSimpleBean extends ISimpleBean {

  public String invokeDestructionCallback();
  
  public String getScopeId();
  
  public boolean isInClusteredSingletonCache();

}
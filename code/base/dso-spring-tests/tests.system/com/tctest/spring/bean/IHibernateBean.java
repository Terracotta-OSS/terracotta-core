/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public interface IHibernateBean {
  public void sharePersistentObj();
  public void shareDetachedObj();
  public void shareObjWithLazyChild(); 
  public void shareLazyObj();
  public void associateSharedObj();
  
  public Integer getSharedId();
  public String getSharedFld();
}

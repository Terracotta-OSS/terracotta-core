/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public class BarServiceImpl implements FooService {

  private transient BarServiceHelper serviceHelper;

  public BarServiceImpl(BarServiceHelper serviceHelper) {
    this.serviceHelper = serviceHelper;
  }

  public String serviceMethod() {
    return serviceHelper.serviceMethod();

  }

}

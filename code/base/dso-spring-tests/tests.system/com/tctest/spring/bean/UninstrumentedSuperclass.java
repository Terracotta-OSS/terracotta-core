/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public class UninstrumentedSuperclass extends SingletonEvent {

  public UninstrumentedSuperclass(Object source, String message) {
    super(source, message);
  }

}

/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public class NonDistributedSingletonEvent extends SingletonEvent {

  public NonDistributedSingletonEvent(Object source, String message) {
    super(source, message);
  }

}

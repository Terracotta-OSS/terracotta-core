/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.context.ApplicationEvent;

public abstract class SingletonEvent extends ApplicationEvent {
  private final String message;

  public SingletonEvent(Object source, String message) {
    super(source);
    this.message = message;
  }

  public String toString() {
    return message + " " + source;
  }
}
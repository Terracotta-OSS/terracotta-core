/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
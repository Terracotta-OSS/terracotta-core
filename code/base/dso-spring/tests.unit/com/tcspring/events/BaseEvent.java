/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tcspring.events;

import org.springframework.context.ApplicationEvent;

public class BaseEvent extends ApplicationEvent {

  public BaseEvent(Object source) {
    super(source);
  }

}

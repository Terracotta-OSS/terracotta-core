/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Date;

public class EventManagerImpl implements EventManager, ApplicationContextAware {

  private SimpleListener     listener;
  private ApplicationContext ctx;

  public EventManagerImpl(SimpleListener listener) {
    this.listener = listener;
  }

  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    this.ctx = ctx;
  }

  public int size() {
    return listener.getEvents().size();
  }

  public void publishEvents(Object source, String message, int count) {
    for (int i = 0; i < count; i++) {
      ctx.publishEvent(new DistributedSingletonEvent(source, message + "[" + i + "]"));
    }
  }

  public void publishLocalEvent(Object source, String message) {
    ctx.publishEvent(new NonDistributedSingletonEvent(source, message));
  }
  
  public void clear() {
    listener.getEvents().clear();
  }

  public Date getLastEventTime() {
    return listener.getLastEventTime();
  }

}

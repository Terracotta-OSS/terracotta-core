/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tctest.spring.bean;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;

public class SimpleListener implements ApplicationListener {
  private transient List events = new ArrayList();

  public synchronized int size() {
    return events.size();
  }

  public synchronized List takeEvents() {
    List returnedValues = events;
    events = new ArrayList();
    return returnedValues;
  }

  public synchronized void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof SingletonEvent) {
      this.events.add(event);
    }
  }

}

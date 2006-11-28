/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */

package com.tctest.spring.bean;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class SimpleListener implements ApplicationListener {
    private transient List events = new ArrayList();
	private Date lastEventTime;

    public List getEvents() {
      return this.events;
    }
    
    // ApplicationListener
    
    public void onApplicationEvent(ApplicationEvent event) {
      if(event instanceof SingletonEvent) {
        this.events.add(event);
        this.lastEventTime = new Date();
      }
    }
    
}


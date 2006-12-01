/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;


public class ActiveBean implements InitializingBean, DisposableBean, Runnable {

  private transient Thread t;
  private transient boolean stopped;
  private transient boolean[] running = new boolean[] { false };

  private List instances = new ArrayList();
  private final Object lock = new Object();
  
  // 
  public List getInstances() {
    synchronized (instances) {
      return instances;
    }
  }
  
  public boolean isStopped() {
    return stopped;
  }
  

	public void afterPropertiesSet() throws Exception {
		this.t = new Thread(this, "ActiveBean for "+Thread.currentThread().getName());
		this.t.start();
	}
  
  public void destroy() throws Exception {
    stopped = true;

    synchronized(running) {
      while (!running[0]) {
        running.wait();
      }
    }
    
    synchronized(instances) {
      instances.remove(0);
    }
  }
  
  public void run() {
    synchronized (this.lock) {
      synchronized(instances) {
        instances.add(Thread.currentThread().getName());
        synchronized(running) {
          running[0] = true;
          running.notifyAll();
        }
      }
      while (!stopped) {
        try {
          Thread.sleep(100L);  // TODO use wait/notifyAll instead
        } catch (InterruptedException e) {
          // ignore
        }
      }
    }
  }
  
}


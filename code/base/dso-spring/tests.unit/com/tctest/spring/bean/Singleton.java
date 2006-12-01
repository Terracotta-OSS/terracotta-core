/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;


public class Singleton implements ISingleton, DisposableBean, InitializingBean {
    private transient String transientValue = "aaa";
    private transient boolean transientBoolean = true;

    private volatile int counter = 0;

    private List recorder;
    private List localRecorder = new ArrayList();
    private transient List transientRecorder = new ArrayList();
    
    // InitializingBean
    public void afterPropertiesSet() throws Exception {
      final String msg = "afterPropertiesSet";
      // System.err.println("### "+Thread.currentThread().getName()+" "+msg+" "+System.identityHashCode(this));
      
      record(recorder, msg);
      record(localRecorder, msg);
      record(transientRecorder, msg);
    }
    
    // DisposableBean
    public void destroy() throws Exception {
      final String msg = "destroy";
      // System.err.println("### "+Thread.currentThread().getName()+" "+msg+" "+System.identityHashCode(this));

      record(recorder, msg);
      record(localRecorder, msg);
      record(transientRecorder, msg);
    }

    private void record(List r, String msg) {

      if(r!=null) {
        synchronized (r) {
          r.add(Thread.currentThread().getName()+" "+msg);
        }
      }
    }
    
    public void setRecorder(List recorder) {
      this.recorder = recorder;
    }
    public List getRecorder() {
      return recorder;
    }
    public List getLocalRecorder() {
      return localRecorder;
    }
    public List getTransientRecorder() {
      return transientRecorder;
    }
    
    // ISingleton
    synchronized public int getCounter() {
      return counter;
    }
    
    synchronized public void incrementCounter() {
        this.counter++;
    }
    
    public String getTransientValue() {
      return transientValue;
    }    
    public void setTransientValue(String transientValue) {
      this.transientValue = transientValue;
    }
    
    public String toString() {
      return "Singleton:"+counter+" "+transientValue;
    }

}


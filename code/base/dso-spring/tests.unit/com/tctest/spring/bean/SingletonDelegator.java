/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;


public class SingletonDelegator extends Singleton {
    
    public int getCounter() {
      return super.getCounter();
    }

    public void incrementCounter() {
      super.incrementCounter();
    }
    
    public String getTransientValue() {
      return super.getTransientValue();
    }
    
    public void setTransientValue(String transientValue) {
      super.setTransientValue(transientValue);
    }

    public String toString() {
      return super.toString();
    }

}


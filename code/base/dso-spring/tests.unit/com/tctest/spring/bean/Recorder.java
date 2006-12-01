/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.ArrayList;
import java.util.List;


public class Recorder {
    
    private List values = new ArrayList();

    public void addValue(String value) {
      synchronized(this) {
        this.values.add(value);
      }
    }
    
    public List getValues() {
      return values;
    }

    public String toString() {
      return System.identityHashCode(this) + " Recorder: " + values;
    }
}


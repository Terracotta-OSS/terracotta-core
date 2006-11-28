/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;


public class State {
  private final String name;

  public State(String name) {
    Assert.assertNotNull(name);
    this.name = name;
  }
  
  public boolean equals(Object o) {
    if(!(o instanceof State)) { return false; }
    return name.equals(((State)o).name);
  }
  
  public int hashCode() {
    return name.hashCode();
  }

  public String toString() {
    return "State[ " + this.name + " ]";
  }
}